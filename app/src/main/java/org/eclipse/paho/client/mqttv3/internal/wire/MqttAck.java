package org.eclipse.paho.client.mqttv3.internal.wire;

public abstract class MqttAck extends MqttWireMessage{

    public MqttAck (byte type){
        super(type);
    }

    @Override
    protected byte getMessageInfo() {
        return 0;
    }

    @Override
    public String toString() {
        return super.toString()+" msgId "+msgId;
    }
}
