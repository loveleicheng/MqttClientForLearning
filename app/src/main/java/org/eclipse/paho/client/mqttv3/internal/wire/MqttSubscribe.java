package org.eclipse.paho.client.mqttv3.internal.wire;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MqttSubscribe extends MqttWireMessage{

    private String[] topics;
    private int[] qos;
    private int count;

    public MqttSubscribe (byte info, byte[] data) throws IOException {
        super(MqttWireMessage.MESSAGE_TYPE_SUBSCRIBE);

        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);
        msgId = dis.readUnsignedShort();

        count = 0;
        topics = new String[10];
        qos = new int[10];
        boolean end = false;
        while (!end) {
            try {
                topics[count] = decodeUTF8(dis);
                qos[count++] = dis.readByte();
            } catch (Exception e) {
                end = true;
            }
        }
        dis.close();
    }

    public MqttSubscribe (String[] topics, int[] qos){
        super(MqttWireMessage.MESSAGE_TYPE_SUBSCRIBE);
        this.topics = topics;
        this.qos = qos;

        if (topics.length != qos.length) {
            throw new IllegalArgumentException();
        }

        for (int i=0;i<qos.length;i++) {
            MqttMessage.validateQos(qos[i]);
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(super.toString());
        sb.append(" names:[");
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("\"").append(topics[i]).append("\"");
        }
        sb.append("] qos:[");
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(qos[i]);
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
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            for (int i=0; i<topics.length; i++) {
                encodeUTF8(dos,topics[i]);
                dos.writeByte(qos[i]);
            }
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new MqttException(ex);
        }
    }

    public boolean isRetryable() {
        return true;
    }
}
