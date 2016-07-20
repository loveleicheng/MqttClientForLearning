package org.eclipse.paho.client.mqttv3.internal;

import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPersistable;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPubRel;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPublish;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Objects;
import java.util.Vector;

public class ClientState {

    private static final String PERSISTENCE_SENT_PREFIX = "s-";
    private static final String PERSISTENCE_CONFIRMED_PREFIX = "sc-";
    private static final String PERSISTENCE_RECEIVED_PREFIX = "r-";

    private static final int DEFAULT_MAX_INFLIGHT = 10;
    private static final int MIN_MSG_ID = 1;
    private static final int MAX_MSG_ID = 65535;
    private int nextMsgId = MIN_MSG_ID - 1;
    private Hashtable inUseMsgIds;

    volatile private Vector pendingMessages;
    volatile private Vector pendingFlows;

    private CommsTokenStore tokenStore;
    private ClientComms clientComms = null;
    private long keepAlive;
    private boolean cleanSession;
    private MqttClientPersistence persistence;

    private int maxInflight = DEFAULT_MAX_INFLIGHT;
    private int actualInFlight = 0;
    private int inFlightPubRels = 0;

    private Object queueLock = new Object();
    private Object quiesceLock = new Object();
    private boolean quiescing = false;

    private long lastOutboundActivity = 0;
    private long lastInboundActivity = 0;
    private long lastPing = 0;
    private MqttWireMessage pingCommand;
    private Object pingOutstandingLock = new Object();
    private int pingOutstanding = 0;

    private boolean connected = false;

    private Hashtable outboundQos2 = null;
    private Hashtable outboundQos1 = null;
    private Hashtable inboundQos2 = null;

    protected void restoreState () throws MqttException {

        Enumeration messageKeys = persistence.keys();
        MqttPersistable persistable;
        String key;
        int highestMsgId = nextMsgId;
        Vector orphanedPubRels = new Vector();
        while (messageKeys.hasMoreElements()){
            key = (String) messageKeys.nextElement();
            persistable = persistence.get(key);
            MqttWireMessage message = restoreMessage(key, persistable);
            if (message != null ){
                if (key.startsWith(PERSISTENCE_RECEIVED_PREFIX)){//接收的qos2 消息

                    inboundQos2.put(new Integer(message.getMessageId()), message);
                }else if (key.startsWith(PERSISTENCE_SENT_PREFIX)){// 发送 qos1/qos2 publish

                    MqttPublish sendMessage = (MqttPublish) message;
                    highestMsgId = Math.max(highestMsgId, sendMessage.getMessageId());

                    if (persistence.containKey(getSendConfirmPersistenceKey(sendMessage))){// qos2 pubrel 已发送
                        MqttPersistable persistedConfirm = persistence.get(getSendConfirmPersistenceKey(sendMessage));
                        MqttPubRel confirmMessage = (MqttPubRel) restoreMessage(key, persistedConfirm);
                        if (confirmMessage != null){
                            confirmMessage.setDuplicate(true);

                            outboundQos2.put(new Integer(confirmMessage.getMessageId()), confirmMessage);
                        }else{
                            //
                        }

                    }else{//qos1 或 qos2但pubrel未发送
                        sendMessage.setDuplicate(true);
                        if (sendMessage.getMessage().getQos() == 2){ //qos2
                            outboundQos2.put(new Integer(sendMessage.getMessageId()),sendMessage);
                        }else{ // qos1
                            outboundQos1.put(new Integer(sendMessage.getMessageId()),sendMessage);
                        }
                    }

                }else if (key.startsWith(PERSISTENCE_CONFIRMED_PREFIX)){//发送 qos2 pubrel
                    MqttPubRel mqttPubRel = (MqttPubRel) message;
                    if (!persistence.containKey(getSendPersistenceKey(mqttPubRel))){
                        orphanedPubRels.addElement(key);
                    }
                }
            }
        }
        messageKeys = orphanedPubRels.elements();
        while (messageKeys.hasMoreElements()){
            key = (String) messageKeys.nextElement();
            persistence.remove(key);
        }

        nextMsgId = highestMsgId;
    }

    private MqttWireMessage restoreMessage (String key, MqttPersistable persistable) throws MqttException {
        MqttWireMessage mqttWireMessage = null;
        try{
            mqttWireMessage = MqttWireMessage.createWireMessage(persistable);
        }catch (Exception e){

        }
        return mqttWireMessage;
    }

    private String getSendPersistenceKey(MqttWireMessage message) {
        return PERSISTENCE_SENT_PREFIX + message.getMessageId();
    }

    private String getSendConfirmPersistenceKey(MqttWireMessage message) {
        return PERSISTENCE_CONFIRMED_PREFIX + message.getMessageId();
    }

    private String getReceivedPersistenceKey(MqttWireMessage message) {
        return PERSISTENCE_RECEIVED_PREFIX + message.getMessageId();
    }

}
