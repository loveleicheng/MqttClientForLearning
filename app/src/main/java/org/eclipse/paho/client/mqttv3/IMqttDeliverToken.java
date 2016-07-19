package org.eclipse.paho.client.mqttv3;

public interface IMqttDeliverToken {

    /**
     * Returns the message associated with this token.
     * <p>Until the message has been delivered, the message being delivered will
     * be returned. Once the message has been delivered <code>null</code> will be
     * returned.
     * @return the message associated with this token or null if already delivered.
     * @throws MqttException if there was a problem completing retrieving the message
     */
    public MqttMessage getMessage() throws MqttException;
}
