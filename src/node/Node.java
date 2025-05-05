package node;

import blockchain.FICBlock;
import blockchain.FICBlockchain;
import blockchain.FTCBlock;
import blockchain.FTCBlockchain;
import models.NodeInfo;
import models.VoteInfo;
import utils.BlockUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

public class Node {
    private static int MIN_PORT_RANGE = 8000;
    private static int MAX_PORT_RANGE = 8999;

    public ServerSocket serverSocket;
    private List<NodeInfo> nodeInfos = new ArrayList<>(); // List of nodes in the network
    private List<NodeInfo> leaders = new ArrayList<>(); // List of elected leaders
    private List<VoteInfo> voteInfos = new ArrayList<>(); // List of votes cast by nodes
    private NodeInfo currentLeader; // Current leader of the network
    private List<List<NodeInfo>> groupedNodes = new ArrayList<>(); // List of nodes grouped by their efficiency and reputation scores

    private String nodeId; // Unique identifier for the node, e.g., username
    private int nodePort;
    private double efficiencyScore;
    private double reputationScore;

    double alpha = 0.5; // Weight for efficiency score
    double beta = 0.5; // Weight for reputation score

    private int commitCount = 0; // Number of commits received
    private int rotationCount = 0; // Tracks the number of rotations

    private final FICBlockchain ficBlockchain = new FICBlockchain(); // Blockchain instance
    private final FTCBlockchain ftcBlockchain = new FTCBlockchain(); // Blockchain instance

    public Node(String nodeId, int nodePort, double efficiencyScore, double reputationScore) {
        this.nodeId = nodeId;
        this.nodePort = nodePort;
        this.efficiencyScore = efficiencyScore;
        this.reputationScore = reputationScore;
    }

    // Start Node
    private void startNode() {
        // Run the server in a separate thread
        new Thread(this::startServer).start();

        // Run handler for user input in a separate thread
//        new Thread(Node::handleUserInput).start();

        // Start node discovery and election process
        while (true) {
            try {
                // Calculate the delay until the next minute starts
                long currentTime = System.currentTimeMillis();
                long nextMinute = ((currentTime / 60000) + 1) * 60000;
                long delay = nextMinute - currentTime;

                System.out.println("[STEP-1] " + nodeId + " Waiting until the next minute..." + delay);
                // Wait until the next minute
                Thread.sleep(delay);

                // This logic wll determine if NEW_ELECTION will be started
                // If rotationCount is 0 or greater than the number of leaders, it means all leaders have been rotated
                if (rotationCount == 0 || rotationCount >= leaders.size()) {
                    System.out.println("[INFO] All leaders have been rotated. Starting new election.");
                    leaders.clear(); // Clear the leaders list
                    currentLeader = null; // Clear the current leader
                    rotationCount = 0; // Reset rotation count
                }

                // If leaders are not yet elected, start a new election
                if (leaders == null || leaders.isEmpty()) {
                    System.out.println("[NEW ELECTION] " + nodeId + " Starting new election...");
                    discoverNodes();
                    electLeader();
                    calculateVotes();
                }

                boolean isInLeaders = leaders.stream().anyMatch(nodeInfo -> nodeInfo.getNodeId().equals(nodeId));

                // If our node is in the leaders list, it will take part in selecting the current leader
                if (isInLeaders) {
                    // This is where we increment the rotation count
                    selectCurrentLeader();
                }

                // If our node is the current leader, it first sends the current leader information to all nodes then creates a new block
                if (currentLeader != null && currentLeader.getNodeId().equals(nodeId)) {

                    // Send the current leader information to all nodes
                    String message = "CURRENT_LEADER-" + currentLeader.getNodeId() + "-" + currentLeader.getNodePort() + "-" + currentLeader.getEfficiencyScore() + "-" + currentLeader.getReputationScore();
                    broadcastMessage(message, nodeInfos);

                    // Group nodes based on their efficiency and reputation scores
                    groupNodes();

                    // Create a new block and add it to the blockchain
                    createFICBlock();
                }
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(nodePort);
            System.out.println("Node started on port: " + nodePort);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClientRequest(clientSocket)).start();
            }
        } catch (Exception e) {
            System.err.println("Error starting server on port " + nodePort);
            e.printStackTrace();
        }
    }

    private void discoverNodes() {
        nodeInfos.clear();
        voteInfos.clear();
        nodeInfos.add(new NodeInfo(nodeId, nodePort, efficiencyScore, reputationScore));

        for (int i = MIN_PORT_RANGE; i <= MAX_PORT_RANGE; i++) {
            try {
                if (i == nodePort) {
                    continue; // Skip the current node's port
                }
                // Attempt to connect to each port in the range
                NodeInfo nodeInfo = getNodeInfo(i);
                if (nodeInfo == null) {
                    continue; // Skip if node is not running
                }
                nodeInfos.add(nodeInfo);
            } catch (Exception e) {
                // Ignore exceptions for ports that are not open
            }
        }

        System.out.println("[STEP-2] " + nodeId + " Discovered nodes size: " + nodeInfos.size());

        if (nodeInfos == null || nodeInfos.isEmpty()) {
            System.err.println("No nodes discovered.");
            return;
        }
    }

    private NodeInfo getNodeInfo(int i) {
        try {
            Socket socket = new Socket("localhost", i);

            // if node is not running then skip
            if (!socket.isConnected()) {
                return null;
            }

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println("GET_NODE_INFO"); // Request to get node info

            // Read the response from the node
            String response = in.readLine();
            String[] infoParts = response.split(",");
            String nodeId = infoParts[0].split(":")[1];
            double efficiencyScore = Double.parseDouble(infoParts[1].split(":")[1]);
            double reputationScore = Double.parseDouble(infoParts[2].split(":")[1]);

            return new NodeInfo(nodeId, i, efficiencyScore, reputationScore);
        } catch (IOException e) {
            // Ignore exceptions for ports that are not open
            return null;
        }
    }

    private void electLeader() {
        if (nodeInfos == null || nodeInfos.isEmpty()) {
            System.err.println("No nodes to elect a leader from.");
            return;
        }

        List<NodeInfo> electedLeaders = new ArrayList<>();

        // Sort nodes based on efficiency and reputation scores
        nodeInfos.sort((n1, n2) -> {
            int efficiencyComparison = Double.compare(n2.getEfficiencyScore(), n1.getEfficiencyScore());
            if (efficiencyComparison != 0) {
                return efficiencyComparison;
            }
            return Double.compare(n2.getReputationScore(), n1.getReputationScore());
        });

        // Elect the 1/5 nodes with the highest scores as leaders
        int numLeaders = Math.max(1, nodeInfos.size() / 5); // 10 nodes, 2 leaders
        for (int i = 0; i < numLeaders; i++) {
            electedLeaders.add(nodeInfos.get(i));
        }

        double voteWeight = alpha * efficiencyScore + beta * reputationScore;

        // Add the leaders nodeId to String format
        String joinedNodeIds = electedLeaders.stream()
                .map(NodeInfo::getNodeId)
                .collect(Collectors.joining(","));

        // Add the current node's vote to the vote list
        voteInfos.add(new VoteInfo(nodeId, joinedNodeIds, voteWeight));

        // Send voting result to all nodes
        String message = "VOTING_RESULT-" + nodeId + "-" + voteWeight + "-" + leaders + "-" + joinedNodeIds;
        // Exclude the current node from the broadcast
        List<NodeInfo> nodesToBroadcast = new ArrayList<>(nodeInfos);
        nodesToBroadcast.removeIf(nodeInfo -> nodeInfo.getNodeId().equals(nodeId));
        broadcastMessage(message, nodesToBroadcast);
        System.out.println("[STEP-3] " + nodeId + " Elected leaders: " + electedLeaders.stream().map(NodeInfo::getNodeId).collect(Collectors.joining(",")));
    }

    private void calculateVotes() {
        int leadersToSelect = Math.max(1, nodeInfos.size() / 5); // 10 nodes, 2 leaders
        Map<String, Double> voteCounts = new HashMap<>();

        // Use a copy of the voteInfos list to avoid concurrent modification
        List<VoteInfo> voteInfosCopy = new ArrayList<>(voteInfos);

        // Iterate through the copied list
        for (VoteInfo voteInfo : voteInfosCopy) {
            String[] joinedNodeIds = voteInfo.getCandidateId().split(",");
            for (String nodeId : joinedNodeIds) {
                voteCounts.put(nodeId, voteCounts.getOrDefault(nodeId, 0.0) + voteInfo.getVoteWeight());
            }
        }

        // Sort the nodes by vote count in descending order and select the top leadersToSelect
        List<String> topLeaders = voteCounts.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(leadersToSelect)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Add the top leaders to the leaders list
        leaders.clear();
        for (String leaderId : topLeaders) {
            nodeInfos.stream()
                    .filter(nodeInfo -> nodeInfo.getNodeId().equals(leaderId))
                    .findFirst()
                    .ifPresent(leaders::add);
        }

        System.out.println("[STEP-4] " + nodeId + " Calculated leaders: " + leaders.stream().map(NodeInfo::getNodeId).collect(Collectors.joining(",")));
    }

    private void selectCurrentLeader() {
        if (leaders == null || leaders.isEmpty()) {
            System.err.println("No leaders to select from.");
            return;
        }

        // Clear leaders if all have been rotated
//        if (rotationCount >= leaders.size()) {
//            // Only current leader can broadcast the RESET message
//            if (!Objects.equals(currentLeader.getNodeId(), nodeId)) {
//                broadcastMessage("RESET", nodeInfos); // Notify other nodes
//            }
//            System.out.println("[INFO] All leaders have been rotated. Clearing leaders list.");
//            leaders.clear();
//            currentLeader = null; // Clear the current leader
//            rotationCount = 0; // Reset rotation count
//            return;
//        }

        // Sort the leaders list by nodeId to ensure consistent order
        leaders.sort(Comparator.comparing(NodeInfo::getNodeId));

        // Select the current leader based on the rotation count
        int leaderIndex = rotationCount % leaders.size(); // 0 % 2 = 0, 1 % 2 = 1
        currentLeader = leaders.get(leaderIndex);
        System.out.println("[STEP-5] " + nodeId + " Current leader selected: " + currentLeader.getNodeId());

        // Increment the rotation count and broadcast it
        rotationCount++;
        broadcastMessage("ROTATION_COUNT-" + rotationCount, nodeInfos);
    }

    private void groupNodes() {
        if (leaders == null || leaders.isEmpty()) {
            System.err.println("[ERROR] Cannot group nodes as there are no leaders.");
            return;
        }

        int noOfGroups = leaders.size(); // Number of groups = number of leaders
        List<NodeInfo> nonLeaderNodes = nodeInfos.stream()
                .filter(node -> !leaders.contains(node)) // Exclude leaders
                .collect(Collectors.toList());

        int groupSize = nonLeaderNodes.size() / noOfGroups; // Base size of each group
        int remainingNodes = nonLeaderNodes.size() % noOfGroups; // Remaining nodes to distribute
        int startIndex = 0;

        groupedNodes.clear(); // Clear previous groups

        // Iterate through the leaders and assign nodes to their respective groups
        for (int i = 0; i < noOfGroups; i++) {
            List<NodeInfo> group = new ArrayList<>();
            group.add(leaders.get(i)); // Add the leader to the group

            // Calculate the end index for the current group
            int endIndex = startIndex + groupSize + (remainingNodes > 0 ? 1 : 0);
            if (remainingNodes > 0) {
                remainingNodes--; // Distribute one extra node to this group
            }

            // Add nodes to the group
            group.addAll(nonLeaderNodes.subList(startIndex, Math.min(endIndex, nonLeaderNodes.size())));

            groupedNodes.add(group); // Add the group to the list of groups
            startIndex = endIndex; // Update start index for the next group
        }

        System.out.println("[STEP-6] " + nodeId + " Grouping done. ");

        // Debugging: Print the grouped nodes
//        for (List<NodeInfo> group : groupedNodes) {
//            System.out.println("Group: " + group.stream().map(NodeInfo::getNodeId).collect(Collectors.joining(", ")));
//        }
    }

    // Create a new block and add it to the blockchain
    private void createFICBlock() {
        String lastBlockHash = ficBlockchain.getLastBlock().getHash();
        String index = String.valueOf(ficBlockchain.getChain().size());
        long timestamp = System.currentTimeMillis();
        String merkleRoot = BlockUtil.calculateMerkleRoot(groupedNodes, voteInfos);
        String hash = BlockUtil.calculateBlockHash(Integer.parseInt(index), timestamp, lastBlockHash, merkleRoot);

        // Create a new block with the gathered information
        FICBlock newBlock = new FICBlock(
                Integer.parseInt(index),
                timestamp,
                groupedNodes,
                voteInfos,
                lastBlockHash,
                merkleRoot,
                hash
        );

        // Add the new block to the blockchain
        try {
            ficBlockchain.addBlock(newBlock);
        } catch (Exception e) {
            System.err.println("Error adding block to blockchain: " + e.getMessage());
        }

        // Broadcast the PRE_PREPARE message
        String message = "PRE_PREPARE-" + newBlock;
        broadcastMessage(message, leaders);
        System.out.println("[STEP-7] " + nodeId + " Created new block: " + newBlock.getHash());
        System.out.println("[PRE_PREPARE] " + nodeId + " Broadcasted PRE_PREPARE message to leaders: " + leaders.stream().map(NodeInfo::getNodeId).collect(Collectors.joining(",")));
    }

    private void handleClientRequest(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String request = in.readLine();
            String[] reqParts = request.split("-");

            if ("GET_NODE_INFO".equals(reqParts[0])) {
                out.println("nodeId:" +  nodeId + ",efficiencyScore:" + efficiencyScore + ",reputationScore:" + reputationScore);
            }

            if ("VOTING_RESULT".equals(reqParts[0])) {
                String[] parts = request.split("-");
                String nodeId = parts[1];
                String voteWeight = parts[2];
                String leaders = parts[3];
                String joinedNodeIds = parts[4];

                // Skip if the response is from the current node
                if (nodeId.equals(this.nodeId)) {
                    return;
                }

                // Check for duplicate votes
                for (VoteInfo voteInfo : voteInfos) {
                    if (voteInfo.getVoterId().equals(nodeId) && voteInfo.getVoteWeight() == Double.parseDouble(voteWeight)) {
                        System.out.println("[INFO] Duplicate vote detected from node: " + nodeId);
                        return;
                    }
                }

                // Add the received vote to the vote list
                voteInfos.add(new VoteInfo(nodeId, joinedNodeIds, Double.parseDouble(voteWeight)));
            }

            if ("CURRENT_LEADER".equals(reqParts[0])) {
                String[] parts = request.split("-");
                String leaderId = parts[1];
                int leaderPort = Integer.parseInt(parts[2]);
                double efficiencyScore = Double.parseDouble(parts[3]);
                double reputationScore = Double.parseDouble(parts[4]);

                // Skip if the response is from the current node
                if (leaderId.equals(this.nodeId)) {
                    return;
                }

                // Update the current leader and last selected leader index
                currentLeader = new NodeInfo(leaderId, leaderPort, efficiencyScore, reputationScore);
            }

            if ("ROTATION_COUNT".equals(reqParts[0])) {
                String[] parts = request.split("-");
                int rotationCount = Integer.parseInt(parts[1]);

                // Skip if the response is from the current node
                if (currentLeader == null || currentLeader.getNodeId().equals(this.nodeId)) {
                    return;
                }

                // Update the rotation count
                this.rotationCount = rotationCount;
            }

            if ("PRE_PREPARE".equals(reqParts[0])) {
                String[] parts = request.split("-", 2);
                String block = parts[1];

                // Check if currentLeader is null
                if (currentLeader == null || (currentLeader.getNodeId().equals(nodeId))) {
                    return; // Skip processing if no leader is selected or the message is from the current leader
                }

                // Phase 2: Prepare - leader sends block to all leaders
                String vote = "PREPARE-" + block;
                broadcastMessage(vote, leaders);
                System.out.println("[PREPARE] " + nodeId + " Received block from leader");
            }

            if ("PREPARE".equals(reqParts[0])) {
                String[] parts = request.split("-", 2);
                String block = parts[1];

                // Check if currentLeader is null
                if (currentLeader == null || (currentLeader.getNodeId().equals(nodeId))) {
                    return; // Skip processing if no leader is selected or the message is from the current leader
                }

                // Phase 3: Commit - leader sends commit to all leaders
                String commit = "COMMIT-" + block;

                // Send commit message only to the current leader
                if (currentLeader != null && !currentLeader.getNodeId().equals(nodeId)) {
                    broadcastMessage(commit, Collections.singletonList(currentLeader));
                    System.out.println("[PREPARE & COMMIT] " + nodeId + " Received prepare message for block and broadcasted commit to current leader: " + currentLeader.getNodeId());
                }
            }

            if ("COMMIT".equals(reqParts[0])) {
                String[] parts = request.split("-", 2);
                String block = parts[1];

                commitCount++;

                // If our node is the current leader and commit count is greater than 2/3 of the leaders, add the block to the blockchain
                if (currentLeader.getNodeId().equals(nodeId) && (leaders.size() == 1 || commitCount >= (leaders.size() * 2) / 3)) {
                    commitCount = 0; // Reset commit count after adding block

                    // Broadcast the newly added block to all nodes
                    String broadcastBlockMessage = "NEW_BLOCK-" + block;
                    broadcastMessage(broadcastBlockMessage, nodeInfos);
                    System.out.println("[COMMIT] " + nodeId + " Recived all and broadcasted NEW_BLOCK message to all nodes");
                }
            }

            if ("RESET".equals(reqParts[0])) {
                System.out.println("[INFO] " + nodeId + " Received request to reset.");
                leaders.clear(); // Clear the leaders list
                currentLeader = null; // Clear the current leader
                rotationCount = 0; // Reset rotation count
            }

           if ("NEW_BLOCK".equals(reqParts[0])) {
                // Skip if the NEW_BLOCK message is from the current leader
                if (currentLeader.getNodeId().equals(nodeId)) {
                     return;
                }

               String serializedBlock = request.substring(request.indexOf("-") + 1); // Extract everything after "NEW_BLOCK-"
               try {
                   ficBlockchain.addBlock(serializedBlock);
                   FICBlock lastBlock = ficBlockchain.getLastBlock();
                   System.out.println("[STEP-9] " + nodeId + " Received new block: " + lastBlock.getHash());
               } catch (Exception e) {
                   System.err.println("Error at: " + nodeId + " Chain error: "+ e.getMessage());
                   e.printStackTrace();
               }
           }

        } catch (Exception e) {
            System.err.println("Error handling client request: " + e.getMessage());
        }
    }

    private void broadcastMessage(String message, List<NodeInfo> nodeInfo) {
        if (nodeInfo == null) {
            System.out.println("Node info is null. Cannot broadcast message.");
            return;
        }

        for (NodeInfo node : nodeInfo) {
            try (Socket socket = new Socket("localhost", node.getNodePort());
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                out.println(message);
            } catch (Exception e) {
                System.err.println("Error sending message to " + node.getNodeId());
                e.printStackTrace();
            }
        }
    }


    // handle user input
    public static void handleUserInput() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter command (type 'help' for available commands):");
        while (true) {
            if (!scanner.hasNextLine()) {
                continue;
            }

            String input = scanner.nextLine();
            String[] parts = input.split(" ");

            String command = parts[0].toLowerCase();

            switch (command) {
                case "help":
                    System.out.println("Available commands:");
                    System.out.println("1. help - Show available commands");
                    System.out.println("2. exit - Exit the program");
                    break;
                case "exit":
                    System.out.println("Exiting...");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Unknown command: " + command);
                    break;
            }

        }
    }


    public static void main(String[] args) {
        if (args.length != 2 || args[0].startsWith("-help")) {
            System.out.println("Usage: java node.Node <nodeId> <nodePort>");
            System.out.println("Example: java node.Node user1 8000");
            System.out.println("Note: Port range is between 8000 and 8999.");
            return;
        }

        String nodeId = args[0];
        int nodePort;
        try {
            nodePort = Integer.parseInt(args[1]);
            if (nodePort < MIN_PORT_RANGE || nodePort > MAX_PORT_RANGE) {
                System.out.println("Port number must be between " + MIN_PORT_RANGE + " and " + MAX_PORT_RANGE);
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number. Please enter a valid integer.");
            return;
        }

        double efficiencyScore = Math.random();
        double reputationScore = Math.random();

        // Make sure user1 and user2 have the same efficiency and reputation scores
        if (nodeId.equals("user1") || nodeId.equals("user2")) {
            efficiencyScore = 0.99;
            reputationScore = 0.99;
        }

        Node node = new Node(nodeId, nodePort, efficiencyScore, reputationScore);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down node: " + nodeId);
            try {
                if (node.serverSocket != null && !node.serverSocket.isClosed()) {
                    node.serverSocket.close();
                }

                // print the blockchain at the end if nodeId is user1 or user2
                if (nodeId.equals("user1") || nodeId.equals("user2")) {
                    System.out.println("Blockchain for " + nodeId + ":");
                    node.ficBlockchain.printBlockchain();
                }

            } catch (Exception e) {
                System.err.println("Error while shutting down: " + e.getMessage());
            }
        }));

        node.startNode();
    }
}

