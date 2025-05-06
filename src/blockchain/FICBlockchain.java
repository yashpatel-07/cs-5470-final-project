package blockchain;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import models.NodeInfo;
import models.VoteInfo;
import utils.BlockUtil;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Integer.parseInt;
import static utils.FindBlockUtil.findBlock;

public class FICBlockchain {
    private List<FICBlock> chain;

    public FICBlockchain() {
        this.chain = new ArrayList<>();
        createGenesisBlock();
    }

    private void createGenesisBlock() {
        List<List<NodeInfo>> genesisNodeInfos = new ArrayList<>();
        List<NodeInfo> groupNodes = new ArrayList<>();
        groupNodes.add(new NodeInfo("genesisNode1", 0, 100, 1.0)); // Example NodeInfo
        groupNodes.add(new NodeInfo("genesisNode2", 0, 100, 1.0)); // Example NodeInfo
        genesisNodeInfos.add(groupNodes); // Example NodeInfo
        List<VoteInfo> genesisVoteInfos = new ArrayList<>();
        genesisVoteInfos.add(new VoteInfo("genesisVoter", "genesisCandidate", 0));

        int index = 0;
        long timestamp = 0L;
        String prevHash = "0";
        String merkleRoot = BlockUtil.calculateMerkleRoot(genesisNodeInfos, genesisVoteInfos);
        String hash = BlockUtil.calculateFICBlockHash(index, timestamp, prevHash, merkleRoot);

        FICBlock genesisBlock = new FICBlock(index, timestamp, genesisNodeInfos, genesisVoteInfos, prevHash, merkleRoot, hash);
        addBlock(genesisBlock);
    }

    public void addBlock(FICBlock block) {
        FICBlock lastBlock = getLastBlock();
        String prevHash = lastBlock != null ? lastBlock.getHash() : "0";

        if (prevHash != null && !prevHash.equals(block.getPrevHash())) {
            throw new IllegalArgumentException("Previous hash does not match the last block's hash.");
        }

        // Validate the chain before adding the new block
        if (!validateChain()) {
            throw new IllegalStateException("Invalid chain. Cannot add new block.");
        }
        chain.add(block);
    }

    public void addBlock(String blockString) {
        if (blockString == null || blockString.trim().isEmpty()) {
            throw new IllegalArgumentException("Block string is null or empty.");
        }

        try {
            // Use Gson to convert the block string into FICBlock object
            Gson gson = new Gson();
            FICBlock newBlock = gson.fromJson(blockString, FICBlock.class);

            // After parsing the block, add it to the blockchain
            addBlock(newBlock);
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Failed to parse block string: " + e.getMessage(), e);
        }
    }

    // Validate the entire chain
    public boolean validateChain() {
        FICBlock currentBlock;
        FICBlock previousBlock;

        for (int i = 1; i < chain.size(); i++) {
            currentBlock = chain.get(i);
            previousBlock = chain.get(i - 1);

            // Check if the current block's hash is valid
            if (!currentBlock.getHash().equals(BlockUtil.calculateFICBlockHash(currentBlock.getIndex(), currentBlock.getTimestamp(), currentBlock.getPrevHash(), currentBlock.getMerkleRoot()))) {
                System.out.println("Invalid hash at block " + currentBlock.getIndex());
                return false;
            }

            // Check if the current block points to the correct previous block
            if (!currentBlock.getPrevHash().equals(previousBlock.getHash())) {
                System.out.println("Invalid previous hash at block " + currentBlock.getIndex());
                return false;
            }
        }

        return true;
    }

    // Print the entire blockchain
    public void printBlockchain() {
        for (FICBlock block : chain) {
            System.out.println("Block Index: " + block.getIndex());
            System.out.println("Timestamp: " + block.getTimestamp());
            System.out.println("Node Infos: " + block.getNodeInfos());
            System.out.println("Vote Infos: " + block.getVoteInfos());
            System.out.println("Merkle Root: " + block.getMerkleRoot());
            System.out.println("Hash: " + block.getHash());
            System.out.println("Previous Hash: " + block.getPrevHash());
            System.out.println("====================================");
        }
    }

    // Getters
    public List<FICBlock> getChain() {
        return chain;
    }

    public FICBlock getBlock(int index) {
        return chain.get(index);
    }

    public FICBlock getLastBlock() {
        if (chain.isEmpty()) {
            return null; // or throw an exception
        }
        return chain.get(chain.size() - 1);
    }

    // Main method to simulate blockchain usage
    public static void main(String[] args) {
        FICBlockchain blockchain = new FICBlockchain();

        // Example 1: Adding a block with node and vote information
        List<List<NodeInfo>> nodeInfos = new ArrayList<>();
        List<NodeInfo> groupNodes = new ArrayList<>();
        groupNodes.add(new NodeInfo("node1", 8080, 95.0, 0.9));
        groupNodes.add(new NodeInfo("node2", 8081, 90.0, 0.8));
        nodeInfos.add(groupNodes);
        List<VoteInfo> voteInfos = new ArrayList<>();
        voteInfos.add(new VoteInfo("voter1", "candidate1", 1.0));
        String merkleRoot = BlockUtil.calculateMerkleRoot(nodeInfos, voteInfos);
        String hash = BlockUtil.calculateFICBlockHash(1, System.currentTimeMillis(), blockchain.getLastBlock().getHash(), merkleRoot);
        FICBlock block = new FICBlock(1, System.currentTimeMillis(), nodeInfos, voteInfos, blockchain.getLastBlock().getHash(), merkleRoot, hash);
        blockchain.addBlock(block);

        // Print the blockchain
        blockchain.printBlockchain();

        // Example of finding a block by index
//        int blockIndexToFind = 1;
//        FICBlock foundBlockByIndex = blockchain.getBlock(blockIndexToFind);
//        if (foundBlockByIndex != null) {
//            System.out.println("Found Block by Index: " + foundBlockByIndex.getNodeInfos());
//        } else {
//            System.out.println("Block not found.");
//        }


        // Example of finding a block by hash
//        String hashToFind = blockchain.getBlock(1).getHash();
//        FICBlock foundBlock = findBlock(blockchain, hashToFind);
//        if (foundBlock != null) {
//            System.out.println("Found Block: " + foundBlock.getNodeInfos());
//        } else {
//            System.out.println("Block not found.");
//        }
    }

    public void replaceChain(List<FICBlock> chain) {
        if (chain.size() > this.chain.size()) {
            this.chain = chain;
        } else {
            System.out.println("Received chain is not longer than the current chain. Ignoring.");
        }
    }
}