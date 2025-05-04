package blockchain;

import models.NodeInfo;
import models.VoteInfo;

import java.util.ArrayList;
import java.util.List;

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
        genesisVoteInfos.add(new VoteInfo("genesisVoter", "genesisCandidate", (int) System.currentTimeMillis())); // Example VoteInfo
        FICBlock genesisBlock = new FICBlock(0, genesisNodeInfos, genesisVoteInfos, "0");
        this.chain.add(genesisBlock);
    }

    public void addBlock(List<List<NodeInfo>> nodeInfos, List<VoteInfo> voteInfos) {
        FICBlock lastBlock = chain.get(chain.size() - 1);
        FICBlock newBlock = new FICBlock(chain.size(), nodeInfos, voteInfos, lastBlock.getHash());
        // Validate the chain before adding the new block
        if (!validateChain()) {
            throw new IllegalStateException("Invalid chain. Cannot add new block.");
        }
        chain.add(newBlock);
    }

    public void addBlock(String blockString) {
        // Parse the blockString to create a new FICBlock
        String[] parts = blockString.replace("FICBlock{", "").replace("}", "").split(", ");
        int index = Integer.parseInt(parts[0].split("=")[1]);
        long timestamp = Long.parseLong(parts[1].split("=")[1]);
        String prevHash = parts[2].split("=")[1].replace("'", "");
        List<List<NodeInfo>> nodeInfos = new FICBlock("").parseNodeInfos(parts[3].split("=")[1]);
        List<VoteInfo> voteInfos = new FICBlock("").parseVoteInfos(parts[4].split("=")[1]);
        String merkleRoot = parts[5].split("=")[1].replace("'", "");
        String hash = parts[6].split("=")[1].replace("'", "");

        FICBlock newBlock = new FICBlock(index, nodeInfos, voteInfos, prevHash);

        // Validate the chain before adding the new block
        if (!validateChain()) {
            throw new IllegalStateException("Invalid chain. Cannot add new block.");
        }

        chain.add(newBlock);
    }

    // Validate the entire chain
    public boolean validateChain() {
        FICBlock currentBlock;
        FICBlock previousBlock;

        for (int i = 1; i < chain.size(); i++) {
            currentBlock = chain.get(i);
            previousBlock = chain.get(i - 1);

            // Check if the hash of the current block is valid
            if (!currentBlock.getHash().equals(currentBlock.calculateHash())) {
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
            System.out.println("Block #" + block.getIndex());
            System.out.println("Timestamp: " + block.getTimestamp());
            System.out.println("Node Infos: " + block.getNodeInfos().toString());
            System.out.println("Vote Infos: " + block.getVoteInfos().toString());
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
        blockchain.addBlock(nodeInfos, voteInfos);

        // Print the blockchain
        blockchain.printBlockchain();

        // Example of finding a block by index
        int blockIndexToFind = 1;
        FICBlock foundBlockByIndex = blockchain.getBlock(blockIndexToFind);
        if (foundBlockByIndex != null) {
            System.out.println("Found Block by Index: " + foundBlockByIndex.getNodeInfos());
        } else {
            System.out.println("Block not found.");
        }


        // Example of finding a block by hash
        String hashToFind = blockchain.getBlock(1).getHash();
        FICBlock foundBlock = findBlock(blockchain, hashToFind);
        if (foundBlock != null) {
            System.out.println("Found Block: " + foundBlock.getNodeInfos());
        } else {
            System.out.println("Block not found.");
        }
    }
}