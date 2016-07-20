package org.eclipse.paho.client.mqttv3;

public class MqttDeliveryToken extends MqttToken implements IMqttDeliveryToken{

    public MqttDeliveryToken() {
        super();
    }

    public MqttDeliveryToken(String logContext) {
        super(logContext);
    }

    @Override
    public MqttMessage getMessage() throws MqttException {
        return internalToken.getMessage();
    }

    protected void setMessage(MqttMessage msg) {
        internalToken.setMessage(msg);
    }
}
