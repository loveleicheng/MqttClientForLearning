package org.eclipse.paho.client.mqttv3;

public class MqttMessage {

    private boolean mutable = true;
    private byte[] payload;

    private int qos = 1;
    private boolean retained = false;
    private boolean dup = false;

    public static void validateQos(int qos) {
        if ((qos < 0) || (qos > 2)) {
            throw new IllegalArgumentException();
        }
    }

    public MqttMessage() {
        setPayload(new byte[]{});
    }

    public MqttMessage(byte[] payload) {
        setPayload(payload);
    }

    public byte[] getPayload() {
        return payload;
    }

    public void clearPayload() {
        checkMutable();
        this.payload = new byte[] {};
    }

    public String toString() {
        return new String(payload);
    }

    public void setPayload(byte[] payload) {
        checkMutable();
        if (payload == null) {
            throw new NullPointerException();
        }
        this.payload = payload;
    }

    protected void setMutable(boolean mutable) {
        this.mutable = mutable;
    }

    protected void checkMutable() throws IllegalStateException {
        if (!mutable) {
            throw new IllegalStateException();
        }
    }

    public int getQos() {
        return qos;
    }

    public boolean isRetained() {
        return retained;
    }

    public void setRetained(boolean retained) {
        checkMutable();
        this.retained = retained;
    }

    public void setQos(int qos) {
        checkMutable();
        validateQos(qos);
        this.qos = qos;
    }

    protected void setDuplicate(boolean dup) {
        this.dup = dup;
    }

    public boolean isDuplicate() {
        return this.dup;
    }

}
