package io.github.VintusS.requests;

public class UpdateLeaderRequest {
    private String leaderHost;
    private int leaderPort;

    public void setLeaderHost(String leaderHost) {
        this.leaderHost = leaderHost;
    }

    public void setLeaderPort(int leaderPort) {
        this.leaderPort = leaderPort;
    }

    public int getLeaderPort() {
        return leaderPort;
    }

    public String getLeaderHost() {
        return leaderHost;
    }
}
