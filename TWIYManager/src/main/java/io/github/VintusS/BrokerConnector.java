package io.github.VintusS;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BrokerConnector {
    private static final String QUEUE_NAME = "random_json_queue";
    private static final String RABBITMQ_HOST = "rabbitmq";
    private static final String RABBITMQ_USER = "twiy";
    private static final String RABBITMQ_PASSWORD = "twiy";
    private static final CountDownLatch shutdownLatch = new CountDownLatch(1);
    private static final Logger logger = LogManager.getLogger(BrokerConnector.class);

    public static void connect() {
        try (ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor()) {
            reconnectExecutor.scheduleWithFixedDelay(() -> {
                try {
                    connectToRabbitMQ();
                } catch (Exception e) {
                    logger.warn("Awaiting broker connection: {}", e.getMessage());
                }
            }, 0, 5, TimeUnit.SECONDS);

            shutdownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Connector interrupted: {}", e.getMessage());
        }
    }

    private static void connectToRabbitMQ() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_HOST);
        factory.setUsername(RABBITMQ_USER);
        factory.setPassword(RABBITMQ_PASSWORD);

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            logger.info("Connected to RabbitMQ and queue declared");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

            channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {});

            shutdownLatch.await();
        }
    }
}