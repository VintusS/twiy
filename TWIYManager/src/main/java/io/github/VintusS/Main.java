package io.github.VintusS;

import io.javalin.Javalin;
import io.javalin.json.JavalinGson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;

public class Main {
    public static Logger logger = LogManager.getLogger(Main.class);
    public static void main(String[] args) {
        new Thread(BrokerConnector::connect, "RabbitMQ-Connection").start();

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinGson());
            config.jetty.modifyWebSocketServletFactory(wsFactoryConfig -> {
                wsFactoryConfig.setIdleTimeout(Duration.ZERO);
            });
        }).start(8081);

        JavalinHttpConfig.setup(app);

        logger.info("Discovery service up and running");
    }
}