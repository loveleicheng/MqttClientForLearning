package org.eclipse.paho.client.mqttv3.internal.wire;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPersistable;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * 传输mqtt消息类型
 */
public abstract class MqttWireMessage {

    public static final byte MESSAGE_TYPE_CONNECT = 1;
    public static final byte MESSAGE_TYPE_CONNACK = 2;
    public static final byte MESSAGE_TYPE_PUBLISH = 3;
    public static final byte MESSAGE_TYPE_PUBACK = 4;
    public static final byte MESSAGE_TYPE_PUBREC = 5;
    public static final byte MESSAGE_TYPE_PUBREL = 6;
    public static final byte MESSAGE_TYPE_PUBCOMP = 7;
    public static final byte MESSAGE_TYPE_SUBSCRIBE = 8;
    public static final byte MESSAGE_TYPE_SUBACK = 9;
    public static final byte MESSAGE_TYPE_UNSUBSCRIBE = 10;
    public static final byte MESSAGE_TYPE_UNSUBACK = 11;
    public static final byte MESSAGE_TYPE_PINGREQ = 12;
    public static final byte MESSAGE_TYPE_PINGRESP = 13;
    public static final byte MESSAGE_TYPE_DISCONNECT = 14;

    protected static final String STRING_ENCODING = "UTF-8";

    private static final String PACKET_NAMES[] = {"reserved", "CONNECT", "CONNACK", "PUBLISH",
            "PUBACK", "PUBREC", "PUBREL", "PUBCOMP", "SUBSCRIBE", "SUBACK",
            "UNSUBSCRIBE", "UNSUBACK", "PINGREQ", "PINGRESP", "DISCONNECT"};

    private byte type;

    protected int msgId;

    protected boolean duplicate = false;

    private byte[] encodedHeader = null;

    public MqttWireMessage (byte type){
        this.type = type;
        this.msgId = 0;
    }

    protected abstract byte getMessageInfo ();

    public byte[] getPayload () throws MqttException {
        return new byte[0];
    }

    public byte getType (){
        return type;
    }

    public int getMessageId() {
        return msgId;
    }

    public void setMessageId(int msgId) {
        this.msgId = msgId;
    }

    protected abstract byte[] getVariableHeader() throws MqttException;

    public static MqttWireMessage createWireMessage(MqttPersistable data) throws MqttException {

        return null;
    }


    /**
     * 与消息关联的键
     */
    public String getKey() {
        return new Integer(getMessageId()).toString();
    }

    protected static byte[] encodeMBI( long number) {
        int numBytes = 0;
        long no = number;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // Encode the remaining length fields in the four bytes
        do {
            byte digit = (byte)(no % 128);
            no = no / 128;
            if (no > 0) {
                digit |= 0x80;
            }
            bos.write(digit);
            numBytes++;
        } while ( (no > 0) && (numBytes<4) );

        return bos.toByteArray();
    }

    public byte[] getHeader () throws MqttException {
        if (encodedHeader == null){
            try{
                int first = ( (getType() & 0x0f) << 4) ^ ( getMessageInfo() & 0x0f );
                byte[] varHeader = getVariableHeader();
                int remLen = varHeader.length + getPayload().length;
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
                dataOutputStream.writeByte(first);
                dataOutputStream.write(encodeMBI(remLen));
                dataOutputStream.write(varHeader);
                dataOutputStream.flush();
                encodedHeader = byteArrayOutputStream.toByteArray();
            }catch (Exception e){
                throw  new MqttException(e);
            }
        }
        return encodedHeader;
    }

    protected byte[] encodeMessageId() throws MqttException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeShort(msgId);
            dos.flush();
            return baos.toByteArray();
        }
        catch (IOException ex) {
            throw new MqttException(ex);
        }
    }

    public boolean isRetryable() {
        return false;
    }

    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

    protected void encodeUTF8(DataOutputStream dos, String stringToEncode) throws MqttException
    {
        try {

            byte[] encodedString = stringToEncode.getBytes("UTF-8");
            byte byte1 = (byte) ((encodedString.length >>> 8) & 0xFF);
            byte byte2 =  (byte) ((encodedString.length >>> 0) & 0xFF);


            dos.write(byte1);
            dos.write(byte2);
            dos.write(encodedString);
        }
        catch(UnsupportedEncodingException ex)
        {
            throw new MqttException(ex);
        } catch (IOException ex) {
            throw new MqttException(ex);
        }
    }

    protected String decodeUTF8(DataInputStream input) throws MqttException
    {
        int encodedLength;
        try {
            encodedLength = input.readUnsignedShort();

            byte[] encodedString = new byte[encodedLength];
            input.readFully(encodedString);

            return new String(encodedString, "UTF-8");
        } catch (IOException ex) {
            throw new MqttException(ex);
        }
    }

    public String toString() {
        return PACKET_NAMES[type];
    }

}












