package org.eclipse.paho.client.mqttv3.internal.wire;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.IOException;

public class MqttPingReq extends MqttWireMessage{

    public static final String KEY = "Ping";

    public MqttPingReq() {
        super(MqttWireMessage.MESSAGE_TYPE_PINGREQ);
    }

    public MqttPingReq(byte info, byte[] variableHeader) throws IOException {
        super(MqttWireMessage.MESSAGE_TYPE_PINGREQ);
    }

    /**
     * Returns <code>false</code> as message IDs are not required for MQTT
     * PINGREQ messages.
     */
    public boolean isMessageIdRequired() {
        return false;
    }

    protected byte[] getVariableHeader() throws MqttException {
        return new byte[0];
    }

    protected byte getMessageInfo() {
        return 0;
    }

    public String getKey() {
        return KEY;
    }

}
