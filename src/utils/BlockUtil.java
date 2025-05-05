package utils;

import models.NodeInfo;
import models.VoteInfo;

import java.util.ArrayList;
import java.util.List;

public class BlockUtil {

    public static String calculateMerkleRoot(List<List<NodeInfo>> nodeInfos, List<VoteInfo> voteInfos) {
        List<String> transactions = new ArrayList<>();

        for (List<NodeInfo> group : nodeInfos) {
            for (NodeInfo node : group) {
                transactions.add(node.toString());
            }
        }

        for (VoteInfo vote : voteInfos) {
            transactions.add(vote.toString());
        }

        while (transactions.size() > 1) {
            List<String> newLevel = new ArrayList<>();
            for (int i = 0; i < transactions.size(); i += 2) {
                String left = transactions.get(i);
                String right = (i + 1 < transactions.size()) ? transactions.get(i + 1) : left;
                newLevel.add(HashUtil.generateSHA256(left + right));
            }
            transactions = newLevel;
        }

        return transactions.isEmpty() ? "" : transactions.get(0);
    }

    public static String calculateBlockHash(int index, long timestamp, String prevHash, String merkleRoot) {
        String dataToHash = index + "|" + timestamp + "|" + prevHash + "|" + merkleRoot;
        return HashUtil.generateSHA256(dataToHash);
    }

    // Expected nodeString format: [[{nodeId:'node1', nodePort:8080, efficiencyScore:95.0, reputationScore:0.9},{nodeId:'node2', nodePort:8081, efficiencyScore:90.0, reputationScore:0.8}]]
    public static List<List<NodeInfo>> parseNodeInfos(String nodeInfosString) {
        List<List<NodeInfo>> nodeInfos = new ArrayList<>();

        if (nodeInfosString == null || nodeInfosString.isEmpty()) {
            return nodeInfos;
        }

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
    public static List<VoteInfo> parseVoteInfos(String voteInfosString) {
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

}

