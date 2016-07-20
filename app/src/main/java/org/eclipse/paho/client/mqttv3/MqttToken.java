package org.eclipse.paho.client.mqttv3;

import org.eclipse.paho.client.mqttv3.internal.Token;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;

public class MqttToken implements IMqttToken{

    public Token internalToken = null;

    public MqttToken (){

    }

    public MqttToken(String logContext) {
        internalToken = new Token(logContext);
    }


    @Override
    public void waitForCompletion() throws MqttException {
        internalToken.waitForCompletion(-1);
    }

    @Override
    public void waitForCompletion(long timeout) throws MqttException {
        internalToken.waitForCompletion(timeout);
    }

    @Override
    public boolean isComplete() {
        return internalToken.isComplete();
    }

    @Override
    public MqttException getException() {
        return internalToken.getException();
    }

    @Override
    public void setActionCallback(IMqttActionListener listener) {
        internalToken.setActionCallback(listener);
    }

    @Override
    public IMqttActionListener getActionCallback() {
        return internalToken.getActionCallback();
    }

    @Override
    public IMqttAsyncClient getClient() {
        return internalToken.getClient();
    }

    @Override
    public String[] getTopics() {
        return internalToken.getTopics();
    }

    @Override
    public void setUserContext(Object userContext) {
        internalToken.setUserContext(userContext);
    }

    @Override
    public Object getUserContext() {
        return internalToken.getUserContext();
    }

    @Override
    public int getMessageId() {
        return internalToken.getMessageID();
    }

    @Override
    public int[] getGrantedQos() {
        return internalToken.getGrantedQos();
    }

    @Override
    public boolean getSessionPresent() {
        return internalToken.getSessionPresent();
    }

    @Override
    public MqttWireMessage getResponse() {
        return internalToken.getResponse();
    }
}
