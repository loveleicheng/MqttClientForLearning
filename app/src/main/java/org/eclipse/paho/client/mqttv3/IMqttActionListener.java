package org.eclipse.paho.client.mqttv3;

public interface IMqttActionListener {

    void onSuccess(IMqttToken asyncActionToken);

    void onFail(IMqttToken asyncActionToken, Throwable exception);
}
