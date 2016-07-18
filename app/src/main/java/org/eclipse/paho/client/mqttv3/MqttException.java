package org.eclipse.paho.client.mqttv3;

public class MqttException extends Exception{

    public static final short REASON_CODE_CLIENT_EXCEPTION              = 0x00;

    private int reasonCode;

    private Throwable cause;

    public MqttException (int reasonCode){
        super();
        this.reasonCode = reasonCode;
    }

    public MqttException (Throwable cause){
        super();
        this.cause = cause;
        this.reasonCode = REASON_CODE_CLIENT_EXCEPTION;
    }

    public MqttException (int reasonCode, Throwable cause){
        super();
        this.reasonCode = reasonCode;
        this.cause = cause;
    }

    public int getReasonCode (){
        return reasonCode;
    }

    public Throwable getCause (){
        return cause;
    }

    public String getMessage (){
        return "";
    }

}
