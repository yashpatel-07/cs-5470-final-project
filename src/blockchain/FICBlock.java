package blockchain;

import models.NodeInfo;
import models.VoteInfo;
import utils.HashUtil;

import java.util.ArrayList;
import java.util.List;

public class FICBlock {
    private final int index;
    private final long timestamp;
    private final String prevHash;
    private final List<List<NodeInfo>> nodeInfos; // Information about nodes (efficiency, reputation)
    private final List<VoteInfo> voteInfos; // Voting records for leadership selection
    private final String merkleRoot; // Root of the Merkle tree of transactions
    private final String hash;

    public FICBlock(int index, List<List<NodeInfo>> nodeInfos, List<VoteInfo> voteInfos, String prevHash) {
        this.index = index;
        this.timestamp = System.currentTimeMillis();
        this.prevHash = prevHash;
        this.nodeInfos = nodeInfos;
        this.voteInfos = voteInfos;
        this.merkleRoot = calculateMerkleRoot();
        this.hash = calculateHash();
    }

    // Constructor for creating a block from a string representation
    public FICBlock(String blockString) {
        String[] parts = blockString.split(", (?=[a-zA-Z]+\\=)");
        this.index = Integer.parseInt(parts[0].split("=")[1]);
        this.timestamp = Long.parseLong(parts[1].split("=")[1]);
        this.prevHash = parts[2].split("=")[1].replace("'", "");
        this.nodeInfos = parseNodeInfos(parts[3].split("=")[1]);
        this.voteInfos = parseVoteInfos(parts[4].split("=")[1]);
        this.merkleRoot = parts[5].split("=")[1].replace("'", "");
        this.hash = parts[6].split("=")[1].replace("'", "").replace("}", "");
    }

    // Expected nodeString format: [[{nodeId:'node1', nodePort:8080, efficiencyScore:95.0, reputationScore:0.9},{nodeId:'node2', nodePort:8081, efficiencyScore:90.0, reputationScore:0.8}]]
    public List<List<NodeInfo>> parseNodeInfos(String nodeInfosString) {
        List<List<NodeInfo>> nodeInfos = new ArrayList<>();

        if (nodeInfosString == null || nodeInfosString.isEmpty()) {
            return nodeInfos;
        }

        // nodeInfosString is a JSON-like array of arrays of NodeInfo objects
        String[] nodeGroups = nodeInfosString.replace("[[", "").replace("]]", "").split("],\\[");
        for (String group : nodeGroups) {
            List<NodeInfo> nodeGroup = new ArrayList<>();
            String[] nodes = group.split("},");
            for (String node : nodes) {
                node = node.endsWith("}") ? node : node + "}";
                String nodeId = node.split("\"nodeId\":\"")[1].split("\"")[0];
                int nodePort = Integer.parseInt(node.split("\"nodePort\":")[1].split(",")[0]);
                double efficiencyScore = Double.parseDouble(node.split("\"efficiencyScore\":")[1].split(",")[0]);
                double reputationScore = Double.parseDouble(node.split("\"reputationScore\":")[1].split("}")[0]);
                nodeGroup.add(new NodeInfo(nodeId, nodePort, efficiencyScore, reputationScore));
            }
            nodeInfos.add(nodeGroup);
        }

        return nodeInfos;
    }

    // Expected voteInfosString format: [{voterId:'voter1', candidateId:'candidate1', voteWeight:1.0},{voterId:'voter2', candidateId:'candidate2', voteWeight:1.0}]
    public List<VoteInfo> parseVoteInfos(String voteInfosString) {
        List<VoteInfo> voteInfos = new ArrayList<>();
        if (voteInfosString == null || voteInfosString.isEmpty()) {
            return voteInfos;
        }

        // Assuming voteInfosString is a JSON-like array of VoteInfo objects
        String[] votes = voteInfosString.replace("[", "").replace("]", "").split("},");
        for (String vote : votes) {
            vote = vote.endsWith("}") ? vote : vote + "}";
            String nodeId = vote.split("\"voterId\":\"")[1].split("\"")[0];
            String joinedNodeIds = vote.split("\"candidateId\":\"")[1].split("\"")[0];
            double voteWeight = Double.parseDouble(vote.split("\"voteWeight\":")[1].split("}")[0]);
            voteInfos.add(new VoteInfo(nodeId, joinedNodeIds, voteWeight));
        }
        return voteInfos;
    }

    // Getters
    public int getIndex() { return index;}

    public long getTimestamp() {
        return timestamp;
    }

    public String getPrevHash() {
        return prevHash;
    }

    public List<List<NodeInfo>> getNodeInfos() {
        return nodeInfos;
    }

    public List<VoteInfo> getVoteInfos() {
        return voteInfos;
    }

    public String getMerkleRoot() {
        return merkleRoot;
    }

    public String getHash() {
        return hash;
    }

    // Calculate the Merkle root of the transactions
    private String calculateMerkleRoot() {
        // Create a list to hold all data for the Merkle tree
        List<String> transactions = new ArrayList<>();

        // Add NodeInfo data
        for (List<NodeInfo> nodeInfoGroup : nodeInfos) {
            for (NodeInfo info : nodeInfoGroup) {
                transactions.add(info.toString());
            }
        }

        // Add VoteInfo data
        for (VoteInfo voteInfo : voteInfos) {
            transactions.add(voteInfo.toString());
        }

        // Calculate the Merkle root
        while (transactions.size() > 1) {
            List<String> newLevel = new ArrayList<>();
            for (int i = 0; i < transactions.size(); i += 2) {
                String left = transactions.get(i);
                String right = (i + 1 < transactions.size()) ? transactions.get(i + 1) : left;
                newLevel.add(HashUtil.generateSHA256(left + right)); // Combine and hash
            }
            transactions = newLevel; // Move to the next level
        }

        return transactions.isEmpty() ? "" : transactions.get(0);
    }

    // Calculate the hash of the block
    public String calculateHash() {
        String dataToHash = index + "|" + timestamp + "|" + prevHash + "|" + merkleRoot;
        return HashUtil.generateSHA256(dataToHash);
    }

    @Override
    public String toString() {
        return "{" +
                "index=" + index +
                ", timestamp=" + timestamp +
                ", prevHash='" + prevHash + '\'' +
                ", nodeInfos=" + nodeInfos +
                ", voteInfos=" + voteInfos +
                ", merkleRoot='" + merkleRoot + '\'' +
                ", hash='" + hash + '\'' +
                '}';
    }
}