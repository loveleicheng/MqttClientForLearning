package org.eclipse.paho.client.mqttv3.internal.wire;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class MqttUnsubAck extends MqttAck{

    public MqttUnsubAck(byte info, byte[] data) throws IOException {
        super(MqttWireMessage.MESSAGE_TYPE_UNSUBACK);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);
        switch (msgId = dis.readUnsignedShort()) {
        }
        dis.close();
    }

    protected byte[] getVariableHeader() throws MqttException {
        // Not needed, as the client never encodes an UNSUBACK
        return new byte[0];
    }
}
