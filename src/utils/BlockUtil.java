package utils;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import models.*;
import com.google.gson.Gson;

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

        // Defensive copy to avoid ConcurrentModificationException
        List<VoteInfo> voteInfosCopy = new ArrayList<>(voteInfos);
        for (VoteInfo vote : voteInfosCopy) {
            transactions.add(vote.toString());
        }

        while (transactions.size() > 1) {
            List<String> newLevel = new ArrayList<>(); // Create a new list for the next level
            for (int i = 0; i < transactions.size(); i += 2) {
                String left = transactions.get(i);
                String right = (i + 1 < transactions.size()) ? transactions.get(i + 1) : left;
                newLevel.add(HashUtil.generateSHA256(left + right));
            }
            transactions = newLevel; // Replace the old list with the new one
        }

        return transactions.isEmpty() ? "" : transactions.get(0);
    }

    public static String calculateFICBlockHash(int index, long timestamp, String prevHash, String merkleRoot) {
        String dataToHash = index + "|" + timestamp + "|" + prevHash + "|" + merkleRoot;
        return HashUtil.generateSHA256(dataToHash);
    }

    public static String calculateFTCBlockHash(int index, long timestamp, FileInfo fileInfo, List<UserInfo> userInfo, Transaction transactions, String prevHash) {
        // Assuming fileInfo, userInfo, and transactions have a proper toString() method
        String fileInfoString = fileInfo != null ? fileInfo.toString() : "";
        String userInfoString = userInfo != null ? userInfo.toString() : "";
        String transactionsString = transactions != null ? transactions.toString() : "";

        String dataToHash = index + "|" + timestamp + "|" + fileInfoString + "|" + userInfoString + "|" + transactionsString + "|" + prevHash;
        return HashUtil.generateSHA256(dataToHash);
    }

    public static List<List<NodeInfo>> parseNodeInfos(String nodeInfosString) {
        if (nodeInfosString == null || nodeInfosString.trim().isEmpty()) {
            return new ArrayList<>(); // Return empty list if the input is null or empty
        }

        try {
            Gson gson = new Gson();
            // Assuming nodeInfos is a list of lists of NodeInfo
            return gson.fromJson(nodeInfosString, new TypeToken<List<List<NodeInfo>>>(){}.getType());
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Failed to parse nodeInfos: " + e.getMessage(), e);
        }
    }

    public static List<VoteInfo> parseVoteInfos(String voteInfosString) {
        if (voteInfosString == null || voteInfosString.trim().isEmpty()) {
            return new ArrayList<>(); // Return empty list if the input is null or empty
        }

        try {
            Gson gson = new Gson();
            return gson.fromJson(voteInfosString, new TypeToken<List<VoteInfo>>(){}.getType());
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Failed to parse voteInfos: " + e.getMessage(), e);
        }
    }

    public static Transaction parseTransaction(String transactionString) {
        try {
            if (transactionString == null || transactionString.isEmpty()) {
                return null;
            }

            Gson gson = new Gson();
            return gson.fromJson(transactionString, Transaction.class);

        } catch (Exception e) {
            System.err.println("Error parsing transaction string: " + e.getMessage());
            return null;
        }
    }

    public static List<UserInfo> parseUserInfos(String userInfosString) {
        try {
            if (userInfosString == null || userInfosString.isEmpty()) {
                return new ArrayList<>();  // Return an empty list if input is empty or null
            }

            Gson gson = new Gson();
            // Use Gson to deserialize the string into a List of UserInfo objects
            return gson.fromJson(userInfosString, new TypeToken<List<UserInfo>>(){}.getType());
        } catch (Exception e) {
            System.err.println("Error parsing user info string: " + e.getMessage());
            return new ArrayList<>();  // Return an empty list in case of an error
        }
    }

    public static FileInfo parseFileInfo(String fileInfoString) {
        try {
            if (fileInfoString == null || fileInfoString.isEmpty()) {
                return null;  // Return null if input is empty or null
            }

            Gson gson = new Gson();
            // Deserialize the fileInfoString into a FileInfo object
            return gson.fromJson(fileInfoString, FileInfo.class);
        } catch (Exception e) {
            System.err.println("Error parsing file info string: " + e.getMessage());
            return null;  // Return null in case of an error
        }
    }

    public static NodeInfo parseNodeInfo(String nodeInfoString) {
        try {
            if (nodeInfoString == null || nodeInfoString.isEmpty()) {
                return null;  // Return null if input is empty or null
            }

            Gson gson = new Gson();
            // Deserialize the nodeInfoString into a NodeInfo object
            return gson.fromJson(nodeInfoString, NodeInfo.class);
        } catch (Exception e) {
            System.err.println("Error parsing node info string: " + e.getMessage());
            return null;  // Return null in case of an error
        }
    }
}

