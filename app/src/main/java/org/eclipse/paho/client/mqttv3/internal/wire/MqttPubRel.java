package org.eclipse.paho.client.mqttv3.internal.wire;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class MqttPubRel extends MqttPersistableWireMessage{

    public MqttPubRel(MqttPubRec pubRec) {
        super(MqttWireMessage.MESSAGE_TYPE_PUBREL);
        this.setMessageId(pubRec.getMessageId());
    }

    public MqttPubRel(byte info, byte[] data) throws IOException {
        super(MqttWireMessage.MESSAGE_TYPE_PUBREL);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);
        msgId = dis.readUnsignedShort();
        dis.close();
    }

    protected byte[] getVariableHeader() throws MqttException {
        return encodeMessageId();
    }

    protected byte getMessageInfo() {
        return (byte)( 2 | (this.duplicate?8:0));
    }

    public String toString() {
        return super.toString() + " msgId " + msgId;
    }
}
