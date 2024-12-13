package io.github.VintusS.raft;

import com.google.gson.Gson;
import io.github.VintusS.Main;
import io.github.VintusS.network.UdpListener;
import io.github.VintusS.network.UdpSender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.*;

public class Raft {
    private static final Logger logger = LogManager.getLogger(Raft.class);

    private enum State {
        FOLLOWER, CANDIDATE, LEADER
    }

    private static final List<String> NODES = Arrays.asList(
            "node1:8105", "node2:8106", "node3:8107", "node4:8108", "node5:8109"
    );

    private final String selfAddress = Main.HOST;
    private final int selfPort = Main.PORT;

    private State currentState = State.FOLLOWER;
    private int currentTerm = 0;
    private String votedFor = null;
    private String currentLeader = null;

    private final Set<String> receivedVotes = ConcurrentHashMap.newKeySet();
    private final Random random = new Random();
    private long electionTimeout = generateElectionTimeout();
    private long lastHeartbeatTime = System.currentTimeMillis();

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
    private final ExecutorService electionExecutor = Executors.newSingleThreadExecutor();

    private final UdpListener listener;
    private final UdpSender sender;
    private final Gson gson = new Gson();

    public Raft() {
        this.listener = new UdpListener(selfPort, this::processMessage);
        this.sender = new UdpSender();

        start();
    }

    private void start() {
        executorService.scheduleAtFixedRate(this::checkElectionTimeout, 50, 50, TimeUnit.MILLISECONDS);
        executorService.scheduleAtFixedRate(this::sendHeartbeats, 100, 100, TimeUnit.MILLISECONDS);

        listener.startListening();
        logger.info("Node {} started", selfAddress);
    }

    private long generateElectionTimeout() {
        return System.currentTimeMillis() + 150 + random.nextInt(150);
    }

    private void checkElectionTimeout() {
        long currentTime = System.currentTimeMillis();

        if (currentState != State.LEADER && currentTime > electionTimeout) {
            startElection();
        }
    }

    private void startElection() {
        currentState = State.CANDIDATE;
        currentTerm++;
        votedFor = selfAddress;
        receivedVotes.clear();
        receivedVotes.add(selfAddress);
        electionTimeout = generateElectionTimeout();

        logger.info("Node {} starting election for term {}", selfAddress, currentTerm);

        electionExecutor.submit(() -> {
            for (String node : NODES) {
                if (!node.equals(selfAddress)) {
                    UdpListener.JsonMessage message = new UdpListener.JsonMessage("VOTE_REQUEST", currentTerm, selfAddress + ":" + selfPort, null);
                    sendMessage(node, message);
                }
            }

            try {
                Thread.sleep(100);
                tallyVotes();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void tallyVotes() {
        long requiredVotes = (NODES.size() / 2) + 1;

        if (receivedVotes.size() >= requiredVotes) {
            becomeLeader();
        } else {
            currentState = State.FOLLOWER;
            votedFor = null;
            electionTimeout = generateElectionTimeout();
            logger.info("Node {} failed to become leader for term {}", selfAddress, currentTerm);
        }
    }

    private void becomeLeader() {
        if (currentState == State.CANDIDATE) {
            currentState = State.LEADER;
            currentLeader = selfAddress;
            logger.info("Node {} became leader for term {}", selfAddress, currentTerm);
            sendHeartbeats();
            notifyManager();
        }
    }

    private void notifyManager() {
        try {
            URL url = new URL("http://manager:8081/update_leader");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String jsonBody = String.format(
                    "{\"leaderHost\": \"%s\", \"leaderPort\": %d}",
                    selfAddress.split(":")[0], selfPort
            );

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = connection.getResponseCode();
        } catch (Exception e) {
            logger.error("Failed to notify manager of leadership: {}", e.getMessage(), e);
        }
    }

    private void sendHeartbeats() {
        if (currentState == State.LEADER) {
            for (String node : NODES) {
                if (!node.equals(selfAddress)) {
                    UdpListener.JsonMessage message = new UdpListener.JsonMessage("HEARTBEAT", currentTerm, selfAddress, null);
                    sendMessage(node, message);
                }
            }
        }
    }

    private void processMessage(UdpListener.JsonMessage message) {
        try {
            switch (message.type) {
                case "VOTE_REQUEST":
                    handleVoteRequest(message.term, message.sender);
                    break;
                case "VOTE_RESPONSE":
                    handleVoteResponse(message.term, message.sender, message.additionalData);
                    break;
                case "HEARTBEAT":
                    handleHeartbeat(message.term, message.sender);
                    break;
            }
        } catch (Exception e) {
            logger.error("Error processing message: {}", gson.toJson(message), e);
        }
    }

    private void handleVoteRequest(int term, String candidate) {
        boolean voteGranted = false;

        if (term > currentTerm) {
            currentTerm = term;
            currentState = State.FOLLOWER;
            votedFor = null;
        }

        if (term == currentTerm && (votedFor == null || votedFor.equals(candidate)) && currentState != State.LEADER) {
            voteGranted = true;
            votedFor = candidate;
            electionTimeout = generateElectionTimeout();
        }

        UdpListener.JsonMessage response = new UdpListener.JsonMessage("VOTE_RESPONSE", term, selfAddress + ":" + selfPort, voteGranted ? "GRANTED" : "REJECTED");
        sendMessage(candidate, response);
    }

    private void handleVoteResponse(int term, String sender, String response) {
        if (currentState != State.CANDIDATE || term != currentTerm) return;

        if ("GRANTED".equals(response)) {
            receivedVotes.add(sender);
        }
    }

    private void handleHeartbeat(int term, String leader) {
        if (term >= currentTerm) {
            currentTerm = term;
            currentState = State.FOLLOWER;
            currentLeader = leader;
            lastHeartbeatTime = System.currentTimeMillis();
            electionTimeout = generateElectionTimeout();
        }
    }

    private void sendMessage(String address, UdpListener.JsonMessage message) {
        try {
            String[] parts = address.split(":");
            if (parts.length != 2) {
                logger.error("Invalid address format: {}", address);
                return;
            }
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            sender.sendMessage(host, port, message);
        } catch (NumberFormatException e) {
            logger.error("Invalid port in address: {}", address, e);
        } catch (Exception e) {
            logger.error("Error sending message to {}: {}", address, e.getMessage(), e);
        }
    }
}
