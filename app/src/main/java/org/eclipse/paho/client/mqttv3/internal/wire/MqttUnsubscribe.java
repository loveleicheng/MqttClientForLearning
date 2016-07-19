package org.eclipse.paho.client.mqttv3.internal.wire;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MqttUnsubscribe extends MqttWireMessage {

    private String[] names;
    private int count;

    public MqttUnsubscribe(String[] names) {
        super(MqttWireMessage.MESSAGE_TYPE_UNSUBSCRIBE);
        this.names = names;
    }

    public MqttUnsubscribe(byte info, byte[] data) throws IOException {
        super(MqttWireMessage.MESSAGE_TYPE_UNSUBSCRIBE);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);
        msgId = dis.readUnsignedShort();

        count = 0;
        names = new String[10];
        boolean end = false;
        while (!end) {
            try {
                names[count] = decodeUTF8(dis);
            } catch (Exception e) {
                end = true;
            }
        }
        dis.close();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(super.toString());
        sb.append(" names:[");
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("\"" + names[i] + "\"");
        }
        sb.append("]");
        return sb.toString();
    }

    protected byte getMessageInfo() {
        return (byte) (2 | (duplicate ? 8 : 0));
    }

    protected byte[] getVariableHeader() throws MqttException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeShort(msgId);
            dos.flush();
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new MqttException(ex);
        }
    }

    public byte[] getPayload() throws MqttException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        for (int i=0; i<names.length; i++) {
            encodeUTF8(dos, names[i]);
        }
        return baos.toByteArray();
    }

    public boolean isRetryable() {
        return true;
    }

}
