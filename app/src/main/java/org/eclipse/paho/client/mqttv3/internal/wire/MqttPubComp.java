package org.eclipse.paho.client.mqttv3.internal.wire;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class MqttPubComp extends MqttAck{

    public MqttPubComp(byte info, byte[] data) throws IOException {
        super(MqttWireMessage.MESSAGE_TYPE_PUBCOMP);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);
        msgId = dis.readUnsignedShort();
        dis.close();
    }

    public MqttPubComp(MqttPublish publish) {
        super(MqttWireMessage.MESSAGE_TYPE_PUBCOMP);
        this.msgId = publish.getMessageId();
    }

    public MqttPubComp(int msgId) {
        super(MqttWireMessage.MESSAGE_TYPE_PUBCOMP);
        this.msgId = msgId;
    }

    protected byte[] getVariableHeader() throws MqttException {
        return encodeMessageId();
    }

}
