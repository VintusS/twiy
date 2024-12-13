package io.github.VintusS;

import io.github.VintusS.raft.Raft;

public class Main {
    public static final String HOST = System.getenv().getOrDefault("HOSTNAME", "localhost");
    public static final int PORT = Integer.parseInt(System.getenv().getOrDefault("UDP_PORT", "8084"));
    public static void main(String[] args) {
        try {
            Raft raft = new Raft();
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
