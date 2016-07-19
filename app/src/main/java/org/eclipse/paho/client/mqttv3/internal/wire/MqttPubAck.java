package org.eclipse.paho.client.mqttv3.internal.wire;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class MqttPubAck extends MqttAck {

    public MqttPubAck(byte info, byte[] data) throws IOException {
        super(MqttWireMessage.MESSAGE_TYPE_PUBACK);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);
        msgId = dis.readUnsignedShort();
        dis.close();
    }

    public MqttPubAck(MqttPublish publish) {
        super(MqttWireMessage.MESSAGE_TYPE_PUBACK);
        msgId = publish.getMessageId();
    }

    protected byte[] getVariableHeader() throws MqttException {
        return encodeMessageId();
    }
}
