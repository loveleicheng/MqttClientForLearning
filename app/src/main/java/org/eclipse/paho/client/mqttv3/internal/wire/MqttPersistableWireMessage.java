package org.eclipse.paho.client.mqttv3.internal.wire;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPersistable;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

public abstract class MqttPersistableWireMessage extends MqttWireMessage implements MqttPersistable{

    public MqttPersistableWireMessage(byte type) {
        super(type);
    }

    @Override
    public byte[] getHeaderBytes() throws MqttPersistenceException {
        try {
            return getHeader();
        }
        catch (Exception e) {
            throw new MqttPersistenceException(e);
        }
    }

    @Override
    public int getHeaderLength() throws MqttPersistenceException {
        return getHeaderBytes().length;
    }

    @Override
    public int getHeaderOffset() throws MqttPersistenceException {
        return 0;
    }

    @Override
    public byte[] getPayloadBytes() throws MqttPersistenceException {
        try {
            return getPayload();
        }
        catch (MqttException ex) {
            throw new MqttPersistenceException(ex.getCause());
        }
    }

    @Override
    public int getPayloadLength() throws MqttPersistenceException {
        return 0;
    }

    @Override
    public int getPayloadOffset() throws MqttPersistenceException {
        return 0;
    }

}
