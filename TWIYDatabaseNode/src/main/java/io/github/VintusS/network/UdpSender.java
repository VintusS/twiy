package io.github.VintusS.network;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class UdpSender {
    private static final Logger logger = LogManager.getLogger(UdpSender.class);
    private final Gson gson = new Gson();

    public void sendMessage(String hostname, int port, Object message) {
        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.configureBlocking(false);
            String jsonMessage = gson.toJson(message);
            ByteBuffer buffer = ByteBuffer.wrap(jsonMessage.getBytes());
            channel.send(buffer, new InetSocketAddress(hostname, port));
            logger.info("Sent message to {}:{} - {}", hostname, port, jsonMessage);
        } catch (IOException e) {
            logger.error("Error sending UDP message: {}", e.getMessage(), e);
        }
    }
}