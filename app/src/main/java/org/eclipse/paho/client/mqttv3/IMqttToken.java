package org.eclipse.paho.client.mqttv3;

import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;

public interface IMqttToken {

    public void waitForCompletion() throws MqttException;

    public void waitForCompletion(long timeout) throws MqttException;

    public boolean isComplete();

    public MqttException getException();

    public void setActionCallback(IMqttActionListener listener);

    public IMqttActionListener getActionCallback();

    public IMqttAsyncClient getClient();

    public String[] getTopics();

    public void setUserContext(Object userContext);

    public Object getUserContext();

    public int getMessageId();

    public int[] getGrantedQos();

    public boolean getSessionPresent();

    public MqttWireMessage getResponse();
}
