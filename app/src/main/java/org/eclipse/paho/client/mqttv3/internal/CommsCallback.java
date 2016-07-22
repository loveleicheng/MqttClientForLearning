package org.eclipse.paho.client.mqttv3.internal;

import org.eclipse.paho.client.mqttv3.ConnectActionListener;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPubAck;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPubComp;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPublish;

import java.util.Vector;

public class CommsCallback implements Runnable{

    private static final String CLASS_NAME = CommsCallback.class.getName();

    private static final int INBOUND_QUEUE_SIZE = 10;
    private MqttCallback mqttCallback;
    private ClientComms clientComms;
    private Vector messageQueue;
    private Vector completeQueue;
    public boolean running = false;
    private boolean quiescing = false;
    private Object lifecycle = new Object();
    private Thread callbackThread;
    private Object workAvailable = new Object();
    private Object spaceAvailable = new Object();
    private ClientState clientState;

    CommsCallback(ClientComms clientComms) {
        this.clientComms = clientComms;
        this.messageQueue = new Vector(INBOUND_QUEUE_SIZE);
        this.completeQueue = new Vector(INBOUND_QUEUE_SIZE);
    }

    public void setClientState(ClientState clientState) {
        this.clientState = clientState;
    }

    /**
     * Starts up the Callback thread.
     */
    public void start(String threadName) {
        synchronized (lifecycle) {
            if (!running) {
                // Praparatory work before starting the background thread.
                // For safety ensure any old events are cleared.
                messageQueue.clear();
                completeQueue.clear();

                running = true;
                quiescing = false;
                callbackThread = new Thread(this, threadName);
                callbackThread.start();
            }
        }
    }

    /**
     * Stops the callback thread.
     * This call will block until stop has completed.
     */
    public void stop() {
        final String methodName = "stop";
        synchronized (lifecycle) {
            if (running) {
                // @TRACE 700=stopping
                running = false;
                if (!Thread.currentThread().equals(callbackThread)) {
                    try {
                        synchronized (workAvailable) {
                            // @TRACE 701=notify workAvailable and wait for run
                            // to finish
                            workAvailable.notifyAll();
                        }
                        // Wait for the thread to finish.
                        callbackThread.join();
                    } catch (InterruptedException ex) {
                    }
                }
            }
            callbackThread = null;
        }
    }

    public void setCallback(MqttCallback mqttCallback) {
        this.mqttCallback = mqttCallback;
    }

    public void run() {
        final String methodName = "run";
        while (running) {
            try {
                // If no work is currently available, then wait until there is some...
                try {
                    synchronized (workAvailable) {
                        if (running && messageQueue.isEmpty()
                                && completeQueue.isEmpty()) {
                            // @TRACE 704=wait for workAvailable
                            workAvailable.wait();
                        }
                    }
                } catch (InterruptedException e) {
                }

                if (running) {
                    // Check for deliveryComplete callbacks...
                    MqttToken token = null;
                    synchronized (completeQueue) {
                        if (!completeQueue.isEmpty()) {
                            // First call the delivery arrived callback if needed
                            token = (MqttToken) completeQueue.elementAt(0);
                            completeQueue.removeElementAt(0);
                        }
                    }
                    if (null != token) {
                        handleActionComplete(token);
                    }

                    // Check for messageArrived callbacks...
                    MqttPublish message = null;
                    synchronized (messageQueue) {
                        if (!messageQueue.isEmpty()) {
                            // Note, there is a window on connect where a publish
                            // could arrive before we've
                            // finished the connect logic.
                            message = (MqttPublish) messageQueue.elementAt(0);

                            messageQueue.removeElementAt(0);
                        }
                    }
                    if (null != message) {
                        handleMessage(message);
                    }
                }

                if (quiescing) {
                    clientState.checkQuiesceLock();
                }

            } catch (Throwable ex) {
                // Users code could throw an Error or Exception e.g. in the case
                // of class NoClassDefFoundError
                running = false;
               // clientComms.shutdownConnection(null, new MqttException(ex));

            } finally {
                synchronized (spaceAvailable) {
                    // Notify the spaceAvailable lock, to say that there's now
                    // some space on the queue...

                    // @TRACE 706=notify spaceAvailable
                    spaceAvailable.notifyAll();
                }
            }
        }
    }

    private void handleActionComplete(MqttToken token)
            throws MqttException {
        final String methodName = "handleActionComplete";
        synchronized (token) {

            // Unblock any waiters and if pending complete now set completed
           // token.internalTok.notifyComplete();

            if (!token.internalToken.isNotified()) {
                // If a callback is registered and delivery has finished
                // call delivery complete callback.
                if ( mqttCallback != null
                        && token instanceof MqttDeliveryToken
                        && token.isComplete()) {
                    mqttCallback.deliveryComplete((MqttDeliveryToken) token);
                }
                // Now call async action completion callbacks
                fireActionEvent(token);
            }

            // Set notified so we don't tell the user again about this action.
            if ( token.isComplete() ){
                if ( token instanceof MqttDeliveryToken || token.getActionCallback() instanceof ConnectActionListener) {
                    token.internalToken.setNotified(true);
                }
            }


            if (token.isComplete()) {
                // Finish by doing any post processing such as delete
                // from persistent store but only do so if the action
                // is complete
                clientState.notifyComplete(token);
            }
        }
    }

    /**
     * This method is called when the connection to the server is lost. If there
     * is no cause then it was a clean disconnect. The connectionLost callback
     * will be invoked if registered and run on the thread that requested
     * shutdown e.g. receiver or sender thread. If the request was a user
     * initiated disconnect then the disconnect token will be notified.
     *
     * @param cause  the reason behind the loss of connection.
     */
    public void connectionLost(MqttException cause) {
        final String methodName = "connectionLost";
        // If there was a problem and a client callback has been set inform
        // the connection lost listener of the problem.
        try {
            if (mqttCallback != null && cause != null) {
                // @TRACE 708=call connectionLost
                mqttCallback.connectionLost(cause);
            }
        } catch (java.lang.Throwable t) {
            // Just log the fact that a throwable has caught connection lost
            // is called during shutdown processing so no need to do anything else
            // @TRACE 720=exception from connectionLost {0}
        }
    }

    /**
     * An action has completed - if a completion listener has been set on the
     * token then invoke it with the outcome of the action.
     *
     * @param token
     */
    public void fireActionEvent(MqttToken token) {
        final String methodName = "fireActionEvent";

        if (token != null) {
            IMqttActionListener asyncCB = token.getActionCallback();
            if (asyncCB != null) {
                if (token.getException() == null) {
                    asyncCB.onSuccess(token);
                } else {
                    asyncCB.onFail(token, token.getException());
                }
            }
        }
    }


    /**
     * This method is called when a message arrives on a topic. Messages are
     * only added to the queue for inbound messages if the client is not
     * quiescing.
     *
     * @param sendMessage
     *            the MQTT SEND message.
     */
    public void messageArrived(MqttPublish sendMessage) {
        final String methodName = "messageArrived";
        if (mqttCallback != null) {
            // If we already have enough messages queued up in memory, wait
            // until some more queue space becomes available. This helps
            // the client protect itself from getting flooded by messages
            // from the server.
            synchronized (spaceAvailable) {
                while (running && !quiescing && messageQueue.size() >= INBOUND_QUEUE_SIZE) {
                    try {
                        // @TRACE 709=wait for spaceAvailable
                        spaceAvailable.wait(200);
                    } catch (InterruptedException ex) {
                    }
                }
            }
            if (!quiescing) {
                messageQueue.addElement(sendMessage);
                // Notify the CommsCallback thread that there's work to do...
                synchronized (workAvailable) {
                    // @TRACE 710=new msg avail, notify workAvailable
                    workAvailable.notifyAll();
                }
            }
        }
    }

    /**
     * Let the call back thread quiesce. Prevent new inbound messages being
     * added to the process queue and let existing work quiesce. (until the
     * thread is told to shutdown).
     */
    public void quiesce() {
        final String methodName = "quiesce";
        this.quiescing = true;
        synchronized (spaceAvailable) {
            // @TRACE 711=quiesce notify spaceAvailable
            // Unblock anything waiting for space...
            spaceAvailable.notifyAll();
        }
    }

    public boolean isQuiesced() {
        if (quiescing && completeQueue.size() == 0 && messageQueue.size() == 0) {
            return true;
        }
        return false;
    }

    private void handleMessage(MqttPublish publishMessage)
            throws MqttException, Exception {
        final String methodName = "handleMessage";
        // If quisecing process any pending messages.
        if (mqttCallback != null) {
            String destName = publishMessage.getTopicName();

            // @TRACE 713=call messageArrived key={0} topic={1}
            //mqttCallback.messageArrived(destName, publishMessage.getMessage());
            if (publishMessage.getMessage().getQos() == 1) {
                this.clientComms.internalSend(new MqttPubAck(publishMessage),
                        new MqttToken(clientComms.getClient().getClientId()));
            } else if (publishMessage.getMessage().getQos() == 2) {
               // this.clientComms.deliveryComplete(publishMessage);
                MqttPubComp pubComp = new MqttPubComp(publishMessage);
                this.clientComms.internalSend(pubComp, new MqttToken(clientComms.getClient().getClientId()));
            }
        }
    }

    public void asyncOperationComplete(MqttToken token) {
        final String methodName = "asyncOperationComplete";

        if (running) {
            // invoke callbacks on callback thread
            completeQueue.addElement(token);
            synchronized (workAvailable) {
                // @TRACE 715=new workAvailable. key={0}
                workAvailable.notifyAll();
            }
        } else {
            // invoke async callback on invokers thread
            try {
                handleActionComplete(token);
            } catch (Throwable ex) {
                // Users code could throw an Error or Exception e.g. in the case
                // of class NoClassDefFoundError
                // @TRACE 719=callback threw ex:

                // Shutdown likely already in progress but no harm to confirm
                //clientComms.shutdownConnection(null, new MqttException(ex));
            }

        }
    }

    /**
     * Returns the thread used by this callback.
     */
    protected Thread getThread() {
        return callbackThread;
    }

}
