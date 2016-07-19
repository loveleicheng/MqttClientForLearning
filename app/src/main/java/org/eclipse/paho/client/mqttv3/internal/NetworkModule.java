package org.eclipse.paho.client.mqttv3.internal;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface NetworkModule {

    public void start() throws IOException, MqttException;

    public InputStream getInputStream() throws IOException;

    OutputStream getOutputStream() throws IOException;

    void stop() throws IOException;

}
