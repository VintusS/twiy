package io.github.VintusS.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.function.Consumer;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class UdpListener {
    private static final Logger logger = LogManager.getLogger(UdpListener.class);
    private final int port;
    private final Consumer<JsonMessage> messageHandler;
    private final Gson gson = new Gson();

    public UdpListener(int port, Consumer<JsonMessage> messageHandler) {
        this.port = port;
        this.messageHandler = messageHandler;
    }

    public void startListening() {
        Thread listenerThread = new Thread(() -> {
            try (Selector selector = Selector.open();
                 DatagramChannel channel = DatagramChannel.open()) {

                channel.bind(new InetSocketAddress(port));
                channel.configureBlocking(false);
                channel.register(selector, SelectionKey.OP_READ);

                logger.info("Listening for UDP messages on port {}", port);
                ByteBuffer buffer = ByteBuffer.allocate(1024);

                while (!Thread.currentThread().isInterrupted()) {
                    selector.select();
                    Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                    while (keys.hasNext()) {
                        SelectionKey key = keys.next();
                        keys.remove();

                        if (key.isReadable()) {
                            DatagramChannel datagramChannel = (DatagramChannel) key.channel();
                            buffer.clear();
                            InetSocketAddress sender = (InetSocketAddress) datagramChannel.receive(buffer);
                            buffer.flip();

                            String message = new String(buffer.array(), 0, buffer.limit()).trim();
                            logger.info("Received message from {}:{} - {}", sender.getHostName(), sender.getPort(), message);
                            try {
                                JsonMessage jsonMessage = gson.fromJson(message, JsonMessage.class);
                                messageHandler.accept(jsonMessage);
                            } catch (JsonSyntaxException e) {
                                logger.error("Invalid JSON received: {}", message, e);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("Error in UDP listener: {}", e.getMessage(), e);
            }
        });

        listenerThread.start();
    }

    public static class JsonMessage {
        public String type;
        public int term;
        public String sender;
        public String additionalData;

        public JsonMessage() {}

        public JsonMessage(String type, int term, String sender, String additionalData) {
            this.type = type;
            this.term = term;
            this.sender = sender;
            this.additionalData = additionalData;
        }
    }
}
