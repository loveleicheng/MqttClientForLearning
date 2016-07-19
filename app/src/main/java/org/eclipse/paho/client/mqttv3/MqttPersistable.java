package org.eclipse.paho.client.mqttv3;

public interface MqttPersistable {

    public byte[] getHeaderBytes() throws MqttPersistenceException;

    public int getHeaderLength() throws MqttPersistenceException;

    public int getHeaderOffset() throws MqttPersistenceException;

    public byte[] getPayloadBytes() throws MqttPersistenceException;

    public int getPayloadLength() throws MqttPersistenceException;

    public int getPayloadOffset() throws MqttPersistenceException;

}
