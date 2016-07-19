package org.eclipse.paho.client.mqttv3.internal.wire;

import org.eclipse.paho.client.mqttv3.MqttException;

public class MqttPingResp extends MqttAck {

    public static final String KEY = "Ping";

    public MqttPingResp(byte info, byte[] variableHeader) {
        super(MqttWireMessage.MESSAGE_TYPE_PINGRESP);
    }

    protected byte[] getVariableHeader() throws MqttException {
        // Not needed, as the client never encodes a PINGRESP
        return new byte[0];
    }

    /**
     * Returns whether or not this message needs to include a message ID.
     */
    public boolean isMessageIdRequired() {
        return false;
    }

    public String getKey() {
        return KEY;
    }
}
