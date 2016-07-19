package org.eclipse.paho.client.mqttv3.internal.wire;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class MqttSuback extends MqttAck{

    private int[] grantedQos;

    public MqttSuback(byte info, byte[] data) throws IOException {
        super(MqttWireMessage.MESSAGE_TYPE_SUBACK);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);
        msgId = dis.readUnsignedShort();
        int index = 0;
        grantedQos = new int[data.length-2];
        int qos = dis.read();
        while (qos != -1) {
            grantedQos[index] = qos;
            index++;
            qos = dis.read();
        }
        dis.close();
    }

    protected byte[] getVariableHeader() throws MqttException {
        // Not needed, as the client never encodes a SUBACK
        return new byte[0];
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(super.toString()).append(" granted Qos");
        for (int i = 0; i < grantedQos.length; ++i) {
            sb.append(" ").append(grantedQos[i]);
        }
        return sb.toString();
    }

    public int[] getGrantedQos() {
        return grantedQos;
    }
}
