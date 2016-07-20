package org.eclipse.paho.client.mqttv3;

public interface IMqttDeliveryToken extends IMqttToken{

    public MqttMessage getMessage() throws MqttException;
}
