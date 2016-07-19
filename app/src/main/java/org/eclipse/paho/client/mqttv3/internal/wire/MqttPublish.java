package org.eclipse.paho.client.mqttv3.internal.wire;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MqttPublish extends MqttPersistableWireMessage{

    private MqttMessage message;
    private String topicName;

    private byte[] encodedPayload = null;

    public MqttPublish (String topicName, MqttMessage message){
        super(MqttWireMessage.MESSAGE_TYPE_PUBLISH);
        this.topicName = topicName;
        this.message = message;
    }

    public MqttPublish (byte info, byte[] data) throws MqttException, IOException {
        super(MqttWireMessage.MESSAGE_TYPE_PUBLISH);
    }

    @Override
    protected byte getMessageInfo() {
        byte info = (byte) (message.getQos() << 1);
        if (message.isRetained()) {
            info |= 0x01;
        }
        if (message.isDuplicate() || duplicate ) {
            info |= 0x08;
        }

        return info;
    }

    public String getTopicName() {
        return topicName;
    }

    public MqttMessage getMessage() {
        return message;
    }

    public byte[] getPayload() throws MqttException {
        if (encodedPayload == null) {
            encodedPayload = encodePayload(message);
        }
        return encodedPayload;
    }

    public int getPayloadLength() {
        int length = 0;
        try {
            length = getPayload().length;
        } catch(MqttException me) {
        }
        return length;
    }

    protected static byte[] encodePayload(MqttMessage message) {
        return message.getPayload();
    }

    @Override
    protected byte[] getVariableHeader() throws MqttException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            encodeUTF8(dos, topicName);
            if (message.getQos() > 0) {
                dos.writeShort(msgId);
            }
            dos.flush();
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new MqttException(ex);
        }
    }

    public boolean isMessageIdRequired() {
        return true;
    }
}
