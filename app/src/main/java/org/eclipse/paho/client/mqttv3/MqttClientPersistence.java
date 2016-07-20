package org.eclipse.paho.client.mqttv3;

import java.util.Enumeration;

public interface MqttClientPersistence {

    void open(String clientId, String serverURI) throws MqttPersistenceException;

    void close() throws MqttPersistenceException;

    void put(String key, MqttPersistable mqttPersistable) throws MqttPersistenceException;

    MqttPersistable get(String key) throws MqttPersistenceException;

    void remove(String key) throws MqttPersistenceException;

    Enumeration keys() throws MqttPersistenceException;

    void clear() throws MqttPersistenceException;

    boolean containKey(String key) throws MqttPersistenceException;

}
