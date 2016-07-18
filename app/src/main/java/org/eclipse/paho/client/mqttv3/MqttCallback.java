package org.eclipse.paho.client.mqttv3;

public interface MqttCallback {

    public void connectionLost (Throwable cause);

    public void messageArrived(String topic)throws Exception;

    public void deliveryComplete(IMqttDeliveryToken token);
}
