package io.github.VintusS;

import java.util.Timer;
import java.util.TimerTask;

public class AddressHolder {
    private String host;
    private int port;

    private static AddressHolder INSTANCE;

    private AddressHolder() {
//        Timer timer = new Timer(true);
//        timer.scheduleAtFixedRate(new TimerTask() {
//            @Override
//            public void run() {
//                Main.logger.info("Host: {}, Port: {}", host, port);
//            }
//        }, 0, 1000);
    }

    public static AddressHolder getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AddressHolder();
        }
        return INSTANCE;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }
}

