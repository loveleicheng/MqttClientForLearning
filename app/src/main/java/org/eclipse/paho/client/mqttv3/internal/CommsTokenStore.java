package org.eclipse.paho.client.mqttv3.internal;

import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPublish;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class CommsTokenStore {

    private Hashtable tokens;
    private String logContext;
    private MqttException closedResponse = null;

    public CommsTokenStore (String logContext){

        this.tokens = new Hashtable();
        this.logContext = logContext;
    }


    public MqttToken getToken (MqttWireMessage message){
        String key = message.getKey();
        return (MqttToken) tokens.get(key);
    }

    public MqttToken getToken (String key){
        return (MqttToken) tokens.get(key);
    }

    public MqttToken removeToken (String key){
        if ( null != key ){
           return (MqttToken) tokens.remove(key);
        }
        return null;
    }

    public MqttToken removeToken (MqttWireMessage message){
        if (message != null){
            return removeToken(message.getKey());
        }
        return null;
    }

    protected MqttDeliveryToken restoreToken (MqttPublish message){
        MqttDeliveryToken token = null;
        synchronized (tokens){
            String key = new Integer(message.getMessageId()).toString();
            if (tokens.contains(key)){
                token = (MqttDeliveryToken) tokens.get(key);
            }else{
                token = new MqttDeliveryToken(logContext);
                token.internalToken.setKey(key);
                tokens.put(key, token);
            }
        }
        return token;
    }

    protected void saveToken (MqttToken token, MqttWireMessage message) throws MqttException {
        synchronized (tokens){
            if (closedResponse == null){
                String key = message.getKey();
                saveToken(token, key);
            }else{
                throw closedResponse;
            }
        }
    }

    protected void saveToken (MqttToken token, String key){
        synchronized (tokens){
            token.internalToken.setKey(key);
            this.tokens.put(key, token);
        }
    }

    protected void quiesce (MqttException e){
        synchronized (tokens){
            closedResponse = e;
        }
    }

    public void open (){
        synchronized (tokens){
            closedResponse = null;
        }
    }

    public void clear (){
        synchronized (tokens){
            tokens.clear();
        }
    }

    public int count() {
        synchronized(tokens) {
            return tokens.size();
        }
    }

    public String toString() {
        String lineSep = System.getProperty("line.separator","\n");
        StringBuffer toks = new StringBuffer();
        synchronized(tokens) {
            Enumeration enumeration = tokens.elements();
            MqttToken token;
            while(enumeration.hasMoreElements()) {
                token = (MqttToken)enumeration.nextElement();
                toks.append("{"+token.internalToken+"}"+lineSep);
            }
            return toks.toString();
        }
    }

    public MqttDeliveryToken[] getOutstandingDelTokens (){
        synchronized (tokens){
            Vector list = new Vector();
            Enumeration enumeration = tokens.elements();
            MqttToken token;
            while (enumeration.hasMoreElements()){
                token = (MqttToken) enumeration.nextElement();
                if ( token != null
                        && token instanceof MqttDeliveryToken
                        && !token.internalToken.isNotified()){
                    list.add(token);
                }
            }
            MqttDeliveryToken[] result = new MqttDeliveryToken[list.size()];
            return (MqttDeliveryToken[]) list.toArray(result);
        }
    }

    public Vector getOutstandingTokens (){
        synchronized (tokens){
            Vector list = new Vector();
            Enumeration enumeration = tokens.elements();
            MqttToken mqttToken;
            while (enumeration.hasMoreElements()){
                mqttToken = (MqttToken) enumeration.nextElement();
                list.add(mqttToken);
            }
            return list;
        }
    }

}
