package org.eclipse.paho.client.mqttv3.internal;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttConnect;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;

public class ClientComms {

    private static final byte CONNECTED	= 0;
    private static final byte CONNECTING	= 1;
    private static final byte DISCONNECTING	= 2;
    private static final byte DISCONNECTED	= 3;
    private static final byte CLOSED	= 4;

    private MqttConnectOptions conOptions;

    private byte conState = DISCONNECTED;

    private IMqttAsyncClient client;

    /**
     * 连接状态同步锁
     */
    private Object	conLock = new Object();
    private boolean	closePending = false;

    public void connect (MqttConnectOptions options, MqttToken token) throws MqttException {

        synchronized (conLock){
            if (isDisconnected() && !closePending){

                conState = CONNECTING;

                this.conOptions = options;

                MqttConnect connect = new MqttConnect(client.getClientId(),
                        options.getMqttVersion(),
                        options.isCleanSession(),
                        options.getKeepAliveInterval(),
                        options.getUserName(),
                        options.getPassword(),
                        options.getWillMessage(),
                        options.getWillDestination());

            }else{  // 不可进行连接的状态
                throw new MqttException(0);
            }
        }

    }
    public boolean isDisconnected() {
        synchronized (conLock) {
            return conState == DISCONNECTED;
        }
    }

    public IMqttAsyncClient getClient() {
        return client;
    }

    private class ConnectBG implements Runnable {

        ClientComms 	clientComms = null;
        Thread 			cBg = null;
        MqttToken 		conToken;
        MqttConnect 	conPacket;

        void start() {
            cBg.start();
        }



        ConnectBG(ClientComms cc, MqttToken cToken, MqttConnect cPacket) {
            clientComms = cc;
            conToken 	= cToken;
            conPacket 	= cPacket;
            cBg = new Thread(this, "MQTT Con: "+getClient().getClientId());
        }

        @Override
        public void run() {

        }
    }

    void internalSend (MqttWireMessage message, MqttToken token) throws MqttException {

    }

}
