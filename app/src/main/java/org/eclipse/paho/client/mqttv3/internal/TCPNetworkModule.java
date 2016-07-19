package org.eclipse.paho.client.mqttv3.internal;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import javax.net.SocketFactory;

public class TCPNetworkModule implements NetworkModule{

    protected Socket socket;
    private SocketFactory socketFactory;
    private String host;
    private int port;
    private int conTimeout;

    public TCPNetworkModule(SocketFactory factory, String host, int port, String resourceContext) {
        this.socketFactory = factory;
        this.host = host;
        this.port = port;

    }

    @Override
    public void start() throws IOException, MqttException {
        try{
            SocketAddress socketAddress = new InetSocketAddress(host, port);
            socket = socketFactory.createSocket();
            socket.connect(socketAddress, conTimeout * 1000);
        }catch (Exception e){
            throw new MqttException(e);
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return socket.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return socket.getOutputStream();
    }

    @Override
    public void stop() throws IOException {
        if (socket != null)
        {
            socket.close();
        }
    }

    public void setConnectTimeout(int timeout) {
        this.conTimeout = timeout;
    }
}
