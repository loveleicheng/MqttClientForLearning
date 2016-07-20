package org.eclipse.paho.client.mqttv3.internal;


import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttAck;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttConnack;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttSuback;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;

public class Token {

    private volatile boolean completed = false;
    private boolean pendingComplete = false;
    private boolean sent = false;

    private Object responseLock = new Object();
    private Object sentLock = new Object();

    protected MqttMessage message = null;
    private MqttWireMessage response = null;
    private MqttException exception = null;
    private String[] topics = null;

    private String key;

    private IMqttAsyncClient client = null;
    private IMqttActionListener callback = null;

    private Object userContext = null;

    private int messageID = 0;
    private boolean notified = false;

    public Token (String logContext){

    }

    public int getMessageID() {
        return messageID;
    }

    public void setMessageID(int messageID) {
        this.messageID = messageID;
    }

    public boolean checkResult() throws MqttException {
        if ( getException() != null)  {
            throw getException();
        }
        return true;
    }

    public MqttException getException() {
        return exception;
    }

    public boolean isComplete() {
        return completed;
    }

    public IMqttAsyncClient getClient() {
        return client;
    }

    protected void setClient(IMqttAsyncClient client) {
        this.client = client;
    }

    protected boolean isCompletePending() {
        return pendingComplete;
    }

    protected boolean isInUse() {
        return (getClient() != null && !isComplete());
    }

    public void setActionCallback(IMqttActionListener listener) {
        this.callback  = listener;

    }
    public IMqttActionListener getActionCallback() {
        return callback;
    }

    public void waitForCompletion () throws MqttException {
        waitForCompletion(-1);
    }

    public void waitForCompletion (long timeout) throws MqttException {
        MqttWireMessage resp = waitForResponse(timeout);
        if (resp == null && !completed){
            exception = new MqttException(0);
        }
        checkResult();
    }

    protected MqttWireMessage waitForResponse () throws MqttException {
        return waitForResponse(-1);
    }

    protected MqttWireMessage waitForResponse (long timeout) throws MqttException {
        synchronized (responseLock){

            while (!this.completed){
                if (this.exception == null){
                    try{
                        if (timeout < 0){
                            responseLock.wait();
                        }else{
                            responseLock.wait(timeout);
                        }

                    }catch (Exception e){
                        exception = new MqttException(e);
                    }
                }

                if (!this.completed){
                    if (this.exception != null){
                        throw exception;
                    }

                    if (timeout > 0){
                        break;
                    }
                }
            }
        }
        return this.response;
    }

    protected void markComplete (MqttWireMessage msg, MqttException ex){

        synchronized (responseLock){
            if (msg instanceof MqttAck){
                this.message =  null;
            }
            this.pendingComplete = true;
            this.response = msg;
            this.exception = ex;
        }
    }

    protected void notifyComplete (){
        synchronized (responseLock){
            if (exception == null && pendingComplete ){
                completed =  true;
                pendingComplete = false;
            }else{
                pendingComplete = false;
            }
            responseLock.notifyAll();
        }

        synchronized (sentLock){
            sent = true;
            sentLock.notifyAll();
        }
    }

    public void waitUntilSent () throws MqttException {
        synchronized (sentLock){
            synchronized (responseLock){
                if (this.exception != null){
                    throw this.exception;
                }
            }
            while (! sent){
                try{
                    sentLock.wait();
                }catch (Exception e){}
            }
            while (! sent){
                throw this.exception;
            }
        }
    }

    protected void notifySent (){
        synchronized (responseLock){
            this.response = null;
            this.completed = false;
        }
        synchronized (sentLock){
            sent = true;
            sentLock.notifyAll();
        }
    }

    public MqttMessage getMessage() {
        return message;
    }

    public MqttWireMessage getWireMessage() {
        return response;
    }


    public void setMessage(MqttMessage msg) {
        this.message = msg;
    }

    public String[] getTopics() {
        return topics;
    }

    public void setTopics(String[] topics) {
        this.topics = topics;
    }

    public Object getUserContext() {
        return userContext;
    }

    public void setUserContext(Object userContext) {
        this.userContext = userContext;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setException(MqttException exception) {
        synchronized(responseLock) {
            this.exception = exception;
        }
    }

    public boolean isNotified() {
        return notified;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }

    public String toString() {
        StringBuffer tok = new StringBuffer();
        tok.append("key=").append(getKey());
        tok.append(" ,topics=");
        if (getTopics() != null) {
            for (int i=0; i<getTopics().length; i++) {
                tok.append(getTopics()[i]).append(", ");
            }
        }
        tok.append(" ,usercontext=").append(getUserContext());
        tok.append(" ,isComplete=").append(isComplete());
        tok.append(" ,isNotified=").append(isNotified());
        tok.append(" ,exception=").append(getException());
        tok.append(" ,actioncallback=").append(getActionCallback());

        return tok.toString();
    }

    public void reset () throws MqttException{
        if (isInUse()){
            throw new MqttException(1);
        }
        client = null;
        completed = false;
        response = null;
        sent = false;
        exception = null;
        userContext = null;
    }

    public int[] getGrantedQos() {
        int[] val = new int[0];
        if (response instanceof MqttSuback) {
            val = ((MqttSuback)response).getGrantedQos();
        }
        return val;
    }

    public boolean getSessionPresent() {
        boolean val = false;
        if (response instanceof MqttConnack) {
            val = ((MqttConnack)response).getSessionPresent();
        }
        return val;
    }

    public MqttWireMessage getResponse() {
        return response;
    }

}
