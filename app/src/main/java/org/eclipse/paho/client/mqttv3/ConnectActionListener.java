package org.eclipse.paho.client.mqttv3;

import org.eclipse.paho.client.mqttv3.internal.ClientComms;

public class ConnectActionListener implements IMqttActionListener{

    private MqttClientPersistence persistence;
    private MqttAsyncClient client;
    private ClientComms comms;
    private MqttConnectOptions options;
    private MqttToken userToken;
    private Object userContext;
    private IMqttActionListener userCallback;
    private int originalMqttVersion;


    public ConnectActionListener(
            MqttAsyncClient client,
            MqttClientPersistence persistence,
            ClientComms comms,
            MqttConnectOptions options,
            MqttToken userToken,
            Object userContext,
            IMqttActionListener userCallback) {
        this.persistence = persistence;
        this.client = client;
        this.comms = comms;
        this.options = options;
        this.userToken = userToken;
        this.userContext = userContext;
        this.userCallback = userCallback;
       // this.originalMqttVersion = options.getMqttVersion();
    }

    public void connect () throws MqttPersistenceException {

    }

    @Override
    public void onSuccess(IMqttToken asyncActionToken) {

    }

    @Override
    public void onFail(IMqttToken asyncActionToken, Throwable exception) {

    }
}
