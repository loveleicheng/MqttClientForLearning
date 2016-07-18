package org.eclipse.paho.client.mqttv3;

public interface IMqttAsyncClient {

    IMqttToken connect() throws MqttException, MqttSecurityException;

    IMqttToken connect(MqttConnectOptions connectOptions) throws MqttException, MqttSecurityException;

    IMqttToken connect(Object userContext, IMqttActionListener callback) throws MqttException, MqttSecurityException;

    IMqttToken connect(MqttConnectOptions connectOptions, Object userContext, IMqttActionListener callback) throws MqttException, MqttSecurityException;


    IMqttToken disconnect () throws MqttException ;

    IMqttToken disconnect (long quiesce) throws MqttException ;

    IMqttToken disconnect (Object userContext, IMqttActionListener callback) throws MqttException;

    IMqttToken disconnect (long quiesce, Object userContext, IMqttActionListener callback) throws MqttException;

    //强制断开连接
    public void disconnectForcibly() throws MqttException;

    public void disconnectForcibly(long disconnectTimeout) throws MqttException;

    public void disconnectForcibly(long quiesceTimeout, long disconnectTimeout) throws MqttException;

    public boolean isConnected();


    public String getClientId();

    String getServerURI();

    //发布消息
    IMqttDeliveryToken publish() throws MqttException, MqttPersistenceException;


    //订阅
    public IMqttToken subscribe (String topicFilter, int qos) throws MqttException;

    //取消订阅
    IMqttToken unsubscribe (String topicFilter, Object userContext, IMqttActionListener actionListener) throws MqttException;

}
