package node;

import blockchain.FICBlock;
import blockchain.FICBlockchain;
import blockchain.FTCBlock;
import blockchain.FTCBlockchain;
import models.NodeInfo;
import models.VoteInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

public class Node {
    private static final int MIN_PORT_RANGE = 8000;
    private static final int MAX_PORT_RANGE = 8999;

    public ServerSocket serverSocket;
    private List<NodeInfo> nodeInfos = new ArrayList<>(); // List of nodes in the network
    private List<NodeInfo> leaders = new ArrayList<>(); // List of elected leaders
    private List<VoteInfo> voteInfos = new ArrayList<>(); // List of votes cast by nodes
    private NodeInfo currentLeader; // Current leader of the network
    private int lastSelectedLeaderIndex = 0; // Index of the last selected leader
    private List<List<NodeInfo>> groupedNodes = new ArrayList<>(); // List of nodes grouped by their efficiency and reputation scores

    private String nodeId; // Unique identifier for the node, e.g., username
    private int nodePort;
    private double efficiencyScore;
    private double reputationScore;

    double alpha = 0.5; // Weight for efficiency score
    double beta = 0.5; // Weight for reputation score

    int commitCount = 0; // Number of commits received

    private FICBlockchain ficBlockchain = new FICBlockchain(); // Blockchain instance
    private FTCBlockchain ftcBlockchain = new FTCBlockchain(); // Blockchain instance

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

        // Start node discovery and election process
        while (true) {
            try {
                // Calculate the delay until the next minute starts
                long currentTime = System.currentTimeMillis();
                long nextMinute = ((currentTime / 60000) + 1) * 60000;
                long delay = nextMinute - currentTime;

                // Wait until the next minute
                Thread.sleep(delay);

                // we don't to run these two unless our leaders are done rotating
                if (lastSelectedLeaderIndex == 0) {
                    discoverNodes();
                    electLeader();
                    calculateVotes();
                }

                boolean isInLeaders = leaders.stream().anyMatch(nodeInfo -> nodeInfo.getNodeId().equals(nodeId));

                // If our node is in the leaders list, it will take part in selecting the current leader
                if (leaders != null && !leaders.isEmpty() && isInLeaders) {
                    selectCurrentLeader();
                }

                // If our node is the current leader, it will perform node grouping
                if (currentLeader != null && currentLeader.getNodeId().equals(nodeId)) {
                    // Group nodes based on their efficiency and reputation scores
                    groupNodes();
                    // Create a new block and add it to the blockchain
                    createBlock("FIC");
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
                // logging
//                System.out.println("Accepted connection from: " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
            }
        } catch (Exception e) {
            System.err.println("Error starting server on port " + nodePort);
            e.printStackTrace();
        }
    }

    private void discoverNodes() {
        nodeInfos.clear();
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
        if (nodeInfos == null || nodeInfos.isEmpty()) {
            System.err.println("No nodes discovered.");
            return;
        }
    }

    private static NodeInfo getNodeInfo(int i) {
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
        broadcastMessage(message, nodeInfos);
    }

    private void calculateVotes() {
        int leadersToSelect = Math.max(1, nodeInfos.size() / 5); // 10 nodes, 2 leaders
        Map<String, Double> voteCounts = new HashMap<>();

        // Iterate through the voteInfos and calculate the total votes for each leader
        for (VoteInfo voteInfo : voteInfos) {
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

        System.out.println("Elected leaders: " + leaders.stream().map(NodeInfo::getNodeId).collect(Collectors.joining(", ")));
    }

    // Select the current leader based on index and track the past leaders
    private void selectCurrentLeader() {
        if (leaders == null || leaders.isEmpty()) {
            System.err.println("No leaders to select from.");
            return;
        }

        // Select the next leader in a round-robin fashion
        lastSelectedLeaderIndex = (lastSelectedLeaderIndex + 1) % leaders.size(); // (0 + 1) % 2 = 1, (1 + 1) % 2 = 0
        currentLeader = leaders.get(lastSelectedLeaderIndex);
        System.out.println("Current leader selected: " + currentLeader.getNodeId() + " with port: " + currentLeader.getNodePort());
    }

    private void groupNodes() {
        // Number of groups = leaders.size()
        int noOfGroups = leaders.size(); // Number of groups to create = number of leaders
        int groupSize = nodeInfos.size() / noOfGroups; // Size of each group = total nodes / number of groups
        int remainingNodes = nodeInfos.size() % noOfGroups; // Remaining nodes after equal distribution
        int startIndex = 0;

        groupedNodes.clear(); // Clear previous groups to avoid accumulation

        // Each leader will be responsible for a group of nodes
        // Group1 will include leader1 and groupSize nodes
        for (int i = 0; i < noOfGroups; i++) {
            List<NodeInfo> group = new ArrayList<>();
            group.add(leaders.get(i)); // Add the leader to the group

            // Calculate the end index for the current group
            int endIndex = startIndex + groupSize;
            if (remainingNodes > 0) {
                endIndex++; // Distribute remaining nodes one by one
                remainingNodes--;
            }

            // Add nodes to the group
            for (int j = startIndex; j < endIndex && j < nodeInfos.size(); j++) {
                // Skip node if it is the leader
                if (nodeInfos.get(j).getNodeId().equals(leaders.get(i).getNodeId())) {
                    continue;
                }
                group.add(nodeInfos.get(j));
            }

            groupedNodes.add(group); // Add the group to the list of groups
            startIndex = endIndex; // Update start index for next group
        }
        // Print the grouped nodes for debugging
        System.out.println("Grouped nodes:");
        for (List<NodeInfo> group : groupedNodes) {
            System.out.println("Group: " + group.stream().map(NodeInfo::getNodeId).collect(Collectors.joining(", ")));
        }
    }

    // Create a new block and add it to the blockchain
    private void createBlock(String blockType) {
        // Create a new block with parameters: index, nodeInfo, voteInfo, prevHash
        FICBlock newBlock = new FICBlock(ficBlockchain.getChain().size(), groupedNodes, voteInfos, ficBlockchain.getLastBlock().getHash());

        // Broadcast the new block to all leaders
        String message = "PRE_PREPARE-" + newBlock;
        broadcastMessage(message, leaders);
    }

    // Consensus algorithm to add a block to the blockchain, FIC or FTC
//    private void addBlockToBlockchain(String block, String blockchainType) {
//        if (blockchainType.equals("FIC")) {
//            try {
//
//
//            } catch (Exception e) {
//                System.err.println("Error adding block to FIC blockchain: " + e.getMessage());
//            }
//        } else if (blockchainType.equals("FTC")) {
//            try {
//
//            } catch (Exception e) {
//                System.err.println("Error adding block to FTC blockchain: " + e.getMessage());
//            }
//        }
//
//    }



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

                // Add the received vote to the vote list
                voteInfos.add(new VoteInfo(nodeId, joinedNodeIds, Double.parseDouble(voteWeight)));
            }

            if ("PRE_PREPARE".equals(reqParts[0])) {
                String[] parts = request.split("-");
                String block = parts[1];
                // Phase 2: Prepare - leader sends vote to all leaders
                String vote = "PREPARE-" + block;
                broadcastMessage(vote, leaders);
            }

            if ("PREPARE".equals(reqParts[0])) {
                String[] parts = request.split("-");
                String block = parts[1];
                // Phase 3: Commit - leader sends commit to all leaders
                String commit = "COMMIT-" + block;
                // Broadcast commit message to current leaders
                List<NodeInfo> currLeader = new ArrayList<>();
                currLeader.add(currentLeader);
                broadcastMessage(commit, currLeader);
            }

            if ("COMMIT".equals(reqParts[0])) {
                String[] parts = request.split("-");
                String block = parts[1];
                commitCount++;

                // if our node is current leader and commit count is greater than 2/3 of the leaders, add the block to the blockchain
                if (Objects.equals(currentLeader.getNodeId(), nodeId) && commitCount >= (leaders.size() * 2) / 3) {
                    ficBlockchain.printBlockchain();
                    commitCount = 0; // Reset commit count after adding block

                    // Broadcast the newly added block to all nodes
                    String broadcastBlockMessage = "NEW_BLOCK-" + ficBlockchain.getChain().get(ficBlockchain.getChain().size() - 1).toString();
                    broadcastMessage(broadcastBlockMessage, nodeInfos);
                }
            }

            if ("NEW_BLOCK".equals(reqParts[0])) {
                String[] parts = request.split("-", 2);
                String serializedBlock = parts[1];

                if (currentLeader.getNodeId().equals(nodeId)) {
                    return;
                }

                try {
                    // Deserialize the block and add it to the local blockchain
                    ficBlockchain.addBlock(serializedBlock);
                    System.out.println("New block added to local FIC blockchain: " + nodeId);
                } catch (Exception e) {
                    System.err.println("Error at NEW_BLOCK: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("Error handling client request: " + e.getMessage());
        }
    }

    private static void broadcastMessage(String message, List<NodeInfo> nodeInfo) {
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
//    public static void handleUserInput() {
//        Scanner scanner = new Scanner(System.in);
//        System.out.println("Enter command (type 'help' for available commands):");
//        while (true) {
//            String command = scanner.nextLine();
//            if (command.equalsIgnoreCase("exit")) {
//                System.out.println("Exiting...");
//                break;
//            } else if (command.equalsIgnoreCase("help")) {
//                System.out.println("Available commands:");
//                System.out.println("1. broadcast - Broadcast a message to all nodes");
//            } else {
//                System.out.println("Unknown command. Type 'help' for available commands.");
//            }
//        }
//    }


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

        // Make sure user1 has the highest efficiency and reputation scores
        if (nodeId.equals("user1")) {
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
            } catch (Exception e) {
                System.err.println("Error while shutting down: " + e.getMessage());
            }
        }));

        node.startNode();
    }
}

