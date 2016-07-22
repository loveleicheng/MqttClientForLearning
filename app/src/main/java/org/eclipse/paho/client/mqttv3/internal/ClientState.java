package org.eclipse.paho.client.mqttv3.internal;

import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistable;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttPingSender;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttAck;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttConnack;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttConnect;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPingReq;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPingResp;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPubAck;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPubComp;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPubRec;
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

    private CommsCallback callback = null;
    private MqttPingSender pingSender = null;

    protected ClientState(MqttClientPersistence persistence, CommsTokenStore tokenStore,
                          CommsCallback callback, ClientComms clientComms, MqttPingSender pingSender) throws MqttException {

        inUseMsgIds = new Hashtable();
        pendingMessages = new Vector(this.maxInflight);
        pendingFlows = new Vector();
        outboundQos2 = new Hashtable();
        outboundQos1 = new Hashtable();
        inboundQos2 = new Hashtable();
        pingCommand = new MqttPingReq();
        inFlightPubRels = 0;
        actualInFlight = 0;

        this.persistence = persistence;
        this.callback = callback;
        this.tokenStore = tokenStore;
        this.clientComms = clientComms;
        this.pingSender = pingSender;

        restoreState();
    }

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

    protected void clearState() throws MqttException {

        persistence.clear();
        inUseMsgIds.clear();
        pendingMessages.clear();
        pendingFlows.clear();
        outboundQos2.clear();
        outboundQos1.clear();
        inboundQos2.clear();
        tokenStore.clear();
    }

    protected void close() {
        inUseMsgIds.clear();
        pendingMessages.clear();
        pendingFlows.clear();
        outboundQos2.clear();
        outboundQos1.clear();
        inboundQos2.clear();
        tokenStore.clear();
        inUseMsgIds = null;
        pendingMessages = null;
        pendingFlows = null;
        outboundQos2 = null;
        outboundQos1 = null;
        inboundQos2 = null;
        tokenStore = null;
        callback = null;
        clientComms = null;
        persistence = null;
        pingCommand = null;
    }

    private void insertInOrder(Vector list, MqttWireMessage newMsg) {
        int newMsgId = newMsg.getMessageId();
        for (int i = 0; i < list.size(); i++) {
            MqttWireMessage otherMsg = (MqttWireMessage) list.elementAt(i);
            int otherMsgId = otherMsg.getMessageId();
            if (otherMsgId > newMsgId) {
                list.insertElementAt(newMsg, i);
                return;
            }
        }
        list.addElement(newMsg);
    }

    public void send (MqttWireMessage message, MqttToken token) throws MqttException {

        if (message.isMessageIdRequired() && message.getMessageId() == 0 ){
            message.setMessageId(getNextMessageId());
        }

        if (token != null){
            try{
                token.internalToken.setMessageID(message.getMessageId());
            }catch(Exception e){}
        }

        if (message instanceof MqttPublish){

            synchronized (queueLock){

                MqttMessage innerMessage = ((MqttPublish)message).getMessage();
                switch (innerMessage.getQos()){
                    case 2:
                        outboundQos2.put(new Integer(message.getMessageId()), message);
                        persistence.put(getSendPersistenceKey(message), (MqttPublish) message);
                    break;

                    case 1:
                        outboundQos1.put(new Integer(message.getMessageId()), message);
                        persistence.put(getSendPersistenceKey(message), (MqttPublish) message);
                    break;
                }
                tokenStore.saveToken(token, message);
                pendingMessages.addElement(message);
                queueLock.notifyAll();
            }

        }else{

            if (message instanceof MqttConnect){
                synchronized (queueLock){
                    tokenStore.saveToken(token, message);
                    pendingFlows.insertElementAt(message, 0);
                    queueLock.notifyAll();
                }
            }else{

                if (message instanceof MqttPingReq){
                    this.pingCommand = message;
                }else if ( message instanceof MqttPubRel ){
                    outboundQos2.put(new Integer(message.getMessageId()), message);
                    persistence.put(getSendConfirmPersistenceKey(message), (MqttPubRel)message);
                }else if (message instanceof MqttPubComp){
                    persistence.remove(getReceivedPersistenceKey(message));
                }

                synchronized (queueLock){
                    if ( ! (message instanceof MqttAck) ){
                        tokenStore.saveToken(token, message);
                    }
                    pendingFlows.addElement(message);
                    queueLock.notifyAll();
                }
            }
        }


    }

    private synchronized int getNextMessageId() throws MqttException {
        return 0;
    }

    protected void undo (MqttPublish message) throws MqttPersistenceException {
        synchronized (queueLock){

            if (message.getMessage().getQos() == 1){
                outboundQos1.remove(new Integer(message.getMessageId()));
            }else {
                outboundQos2.remove(new Integer(message.getMessageId()));
            }
            pendingMessages.removeElement(message);
            persistence.remove(getSendPersistenceKey(message));
            tokenStore.removeToken(message);
            checkQuiesceLock();
        }
    }

    private void decrementInFlight (){
        synchronized (queueLock){
            actualInFlight--;
            if (!checkQuiesceLock()){
                queueLock.notifyAll();
            }
        }
    }
    private synchronized void releaseMessageId(int msgId) {
        inUseMsgIds.remove(new Integer(msgId));
    }

    protected void notifySent (MqttWireMessage message){

        MqttToken token = tokenStore.getToken(message);
        token.internalToken.notifySent();
        if (message instanceof MqttPingReq){
            //心跳信息更新
        }else if (message instanceof MqttPublish){
            if (((MqttPublish)message).getMessage().getQos() == 0){
                token.internalToken.markComplete(null, null);
                //callback 回调
                decrementInFlight();
                releaseMessageId(message.getMessageId());
                tokenStore.removeToken(message);
                checkQuiesceLock();
            }
        }
    }

    protected MqttWireMessage get() throws MqttException{
        MqttWireMessage result = null;
        synchronized (queueLock){
            while (result == null){

                if ( (pendingMessages.isEmpty() && pendingFlows.isEmpty())
                        || (pendingFlows.isEmpty() && actualInFlight >= maxInflight) ){
                    try{
                        queueLock.wait();
                    }catch (Exception e){}
                }

                if ( !connected && ( pendingFlows.isEmpty() || !( (MqttWireMessage)pendingFlows.elementAt(0) instanceof MqttConnect ) )){
                    return null;
                }

                if (!pendingFlows.isEmpty()){
                    result = (MqttWireMessage) pendingFlows.remove(0);
                    if (result instanceof MqttPubRel){
                        inFlightPubRels++;
                    }
                    checkQuiesceLock();
                }else if (!pendingMessages.isEmpty()){
                    if (actualInFlight < maxInflight){
                        result = (MqttWireMessage) pendingMessages.elementAt(0);
                        pendingMessages.remove(0);
                        actualInFlight++;
                    }
                }

            }
        }
        return result;
    }

    protected boolean checkQuiesceLock() {
        return true;
    }

    public void connected (){
        connected = true;
        //start ping sender thread
    }

    protected void notifyReceivedAck (MqttAck ack) throws MqttException {

        MqttToken token = tokenStore.getToken(ack);
        MqttException mex = null;

        if ( ack instanceof MqttPubRec){
            // callback.fireActionEvent
            MqttPubRel rel = new MqttPubRel((MqttPubRec) ack);
            this.send(rel, token);
        }else if ( ack instanceof MqttPubAck || ack instanceof  MqttPubComp){
            notifyResult(ack, token, mex);
        }else if (ack instanceof MqttPingResp){
            //ping 相关
        }else if (ack instanceof MqttConnack){// 连接

            int rc = ((MqttConnack)ack).getReturnCode();
            if (rc == 0){
                synchronized (queueLock){
                    if (cleanSession){
                        clearState();
                        tokenStore.saveToken(token, ack);
                    }
                    inFlightPubRels = 0;
                    actualInFlight = 0;
                    restoreInflightMessages();
                    connected();
                }


            }else{
                mex = new MqttException(0);
                throw mex;
            }

            //clientComms.connectComplete
            notifyResult(ack, token, mex);
            tokenStore.removeToken(ack);

            synchronized (queueLock){
                queueLock.notifyAll();
            }

        }else {
            //订阅 取消订阅
            notifyResult(ack, token, mex);
            releaseMessageId(ack.getMessageId());
            tokenStore.removeToken(ack);
        }

    }

    protected void notifyReceivedMsg (MqttWireMessage message) throws MqttException {
        if (!quiescing){
            if (message instanceof MqttPublish){
                MqttPublish send = (MqttPublish) message;
                switch (send.getMessage().getQos()){
                    case 0:
                    case 1:
                        //callback.messageArrived
                        break;
                    case 2:
                        persistence.put(getReceivedPersistenceKey(message), (MqttPublish)message);
                        inboundQos2.put( new Integer(send.getMessageId()), send);
                        this.send(new MqttPubRec(send), null);
                        break;
                }
            } else if (message instanceof MqttPubRel){
                MqttPublish sendMsg = (MqttPublish) inboundQos2.get(new Integer(message.getMessageId()));
                if (sendMsg != null){
                    if (callback != null){
                        //callback messageArrived
                    }
                } else {
                    MqttPubComp pubComp = new MqttPubComp(message.getMessageId());
                    this.send(pubComp, null);
                }
            }
        }
    }

    protected void notifyComplete (MqttToken token) throws MqttException {
        MqttWireMessage message = token.internalToken.getWireMessage();

        if ( message != null && message instanceof MqttAck ){

            MqttAck ack = (MqttAck) message;

            if (ack instanceof MqttPubAck){
                persistence.remove(getSendPersistenceKey(message));
                outboundQos1.remove(new Integer(ack.getMessageId()));
                decrementInFlight();
                releaseMessageId(message.getMessageId());
                tokenStore.removeToken(message);

            } else if ( ack instanceof MqttPubComp ){

                persistence.remove(getSendPersistenceKey(message));
                persistence.remove(getSendConfirmPersistenceKey(message));
                outboundQos2.remove(new Integer(ack.getMessageId()));

                inFlightPubRels--;
                decrementInFlight();
                releaseMessageId(message.getMessageId());
                tokenStore.removeToken(message);
            }

            checkQuiesceLock();
        }
    }

    protected void notifyResult (MqttWireMessage ack, MqttToken token, MqttException ex){

        if ( ack != null && ack instanceof MqttPubAck && !( ack instanceof MqttPubRec ) ){

            //callback asyncOperationComplete
        }

        if ( ack == null ){
            //callback asyncOperationComplete
        }

    }

    public void disconnected (MqttException reason){

        this.connected = false;
        try{
            if (cleanSession){
                clearState();
            }
            pendingMessages.clear();
            pendingFlows.clear();
            synchronized (pingOutstandingLock){
                pingOutstanding = 0;
            }

        }catch (Exception e){

        }

    }

    private Vector reOrder(Vector list) {

        // here up the new list
        Vector newList = new Vector();

        if (list.size() == 0) {
            return newList; // nothing to reorder
        }

        int previousMsgId = 0;
        int largestGap = 0;
        int largestGapMsgIdPosInList = 0;
        for (int i = 0; i < list.size(); i++) {
            int currentMsgId = ((MqttWireMessage) list.elementAt(i)).getMessageId();
            if (currentMsgId - previousMsgId > largestGap) {
                largestGap = currentMsgId - previousMsgId;
                largestGapMsgIdPosInList = i;
            }
            previousMsgId = currentMsgId;
        }
        int lowestMsgId = ((MqttWireMessage) list.elementAt(0)).getMessageId();
        int highestMsgId = previousMsgId; // last in the sorted list

        // we need to check that the gap after highest msg id to the lowest msg id is not beaten
        if (MAX_MSG_ID - highestMsgId + lowestMsgId > largestGap) {
            largestGapMsgIdPosInList = 0;
        }

        // starting message has been located, let's start from this point on
        for (int i = largestGapMsgIdPosInList; i < list.size(); i++) {
            newList.addElement(list.elementAt(i));
        }

        // and any wrapping back to the beginning
        for (int i = 0; i < largestGapMsgIdPosInList; i++) {
            newList.addElement(list.elementAt(i));
        }

        return newList;
    }

    private void restoreInflightMessages() {

        final String methodName = "restoreInflightMessages";
        pendingMessages = new Vector(this.maxInflight);
        pendingFlows = new Vector();

        Enumeration keys = outboundQos2.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object msg = outboundQos2.get(key);
            if (msg instanceof MqttPublish) {
                //@TRACE 610=QoS 2 publish key={0}

                insertInOrder(pendingMessages, (MqttPublish)msg);
            } else if (msg instanceof MqttPubRel) {
                //@TRACE 611=QoS 2 pubrel key={0}

                insertInOrder(pendingFlows, (MqttPubRel)msg);
            }
        }
        keys = outboundQos1.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            MqttPublish msg = (MqttPublish)outboundQos1.get(key);
            //@TRACE 612=QoS 1 publish key={0}

            insertInOrder(pendingMessages, msg);
        }

        this.pendingFlows = reOrder(pendingFlows);
        this.pendingMessages = reOrder(pendingMessages);

    }

    public void notifyQueueLock() {
        synchronized (queueLock) {
            queueLock.notifyAll();
        }
    }

    public void quiesce(long timeout) {
        if (timeout > 0 ) {
            synchronized (queueLock) {
                this.quiescing = true;
            }
            //callback.quiesce();
            notifyQueueLock();

            synchronized (quiesceLock) {
                try {
                    int tokc = tokenStore.count();
                    if (tokc > 0 || pendingFlows.size() >0 /*|| !callback.isQuiesced() */) {

                        quiesceLock.wait(timeout);
                    }
                }
                catch (InterruptedException ex) {
                }
            }

            synchronized (queueLock) {
                pendingMessages.clear();
                pendingFlows.clear();
                quiescing = false;
                actualInFlight = 0;
            }
        }
    }

    protected void deliveryComplete(MqttPublish message) throws MqttPersistenceException {

        persistence.remove(getReceivedPersistenceKey(message));
        inboundQos2.remove(new Integer(message.getMessageId()));
    }

}
