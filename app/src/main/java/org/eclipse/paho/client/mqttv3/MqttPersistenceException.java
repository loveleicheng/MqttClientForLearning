package org.eclipse.paho.client.mqttv3;

public class MqttPersistenceException extends MqttException{

    public MqttPersistenceException(int reasonCode) {
        super(reasonCode);
    }

    public MqttPersistenceException(Throwable cause) {
        super(cause);
    }
}
