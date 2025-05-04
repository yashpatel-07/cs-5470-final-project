package models;

public class NodeInfo {
    private String nodeId; // Unique identifier for the node, e.g., username
    private int nodePort; // Port number for the node
    private double efficiencyScore;
    private double reputationScore;

    public NodeInfo(String nodeId,int nodePort, double efficiencyScore, double reputationScore) {
        this.nodeId = nodeId;
        this.nodePort = nodePort;
        this.efficiencyScore = efficiencyScore;
        this.reputationScore = reputationScore;
    }

    @Override
    public String toString() {
        return "{" +
                "\"nodeId\":\"" + nodeId + "\"," +
                "\"nodePort\":" + nodePort + "," +
                "\"efficiencyScore\":" + efficiencyScore + "," +
                "\"reputationScore\":" + reputationScore +
                "}";
    }

    // Getters and Setters
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public int getNodePort() { return nodePort; }
    public void setNodePort(int nodePort) { this.nodePort = nodePort; }

    public double getEfficiencyScore() { return efficiencyScore; }
    public void setEfficiencyScore(double efficiencyScore) { this.efficiencyScore = efficiencyScore; }

    public double getReputationScore() { return reputationScore; }
    public void setReputationScore(double reputationScore) { this.reputationScore = reputationScore; }
}