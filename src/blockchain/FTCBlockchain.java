package blockchain;

import models.Transaction;
import models.UserInfo;
import models.FileInfo;
import utils.BlockUtil;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class FTCBlockchain {
    private final List<FTCBlock> chain;

    public FTCBlockchain() {
        this.chain = new ArrayList<>();
        createGenesisBlock();
    }

    private void createGenesisBlock() {
        // Create the genesis block with dummy data
        int index = 0;
        long timestamp = 0L;
        FileInfo fileInfo = new FileInfo("genesis.txt", "genesisHashCID", "eFileKey");
        List<UserInfo> userInfos = new ArrayList<>();
        userInfos.add(new UserInfo("genesisPublicKey", "genesisEncryptedKey"));
        Transaction transactions = new Transaction(null, null, "fileName", "CID", "sendersPublicKey", "reciversPublicKey", "eFileKey", "none", "creatorSign", "validatorSign");
        // Add the genesis block to the chain
        FTCBlock genesisBlock = new FTCBlock(index, timestamp, fileInfo, userInfos, transactions, "0", BlockUtil.calculateFTCBlockHash(index, timestamp, fileInfo, userInfos, transactions, "0"));
        chain.add(genesisBlock);
    }

    public void addBlock(FTCBlock block) {
        FTCBlock lastBlock = getLastBlock();
        String prevHash = lastBlock != null ? lastBlock.getHash() : "0";

        if (prevHash != null && !prevHash.equals(block.getPrevHash())) {
            throw new IllegalArgumentException("Previous hash does not match the last block's hash.");
        }

        // Validate the chain before adding the new block
        if (!validateChain()) {
            throw new IllegalArgumentException("Invalid chain. Cannot add new block.");
        }

        chain.add(block);
    }

    // Using Gson to parse the block string
    public void addBlock(String blockString) {
        if (blockString == null || blockString.trim().isEmpty()) {
            throw new IllegalArgumentException("Block string is null or empty.");
        }

        try {
            Gson gson = new Gson();
            FTCBlock block = gson.fromJson(blockString, FTCBlock.class);
            // Validate the new block before adding it to the chain
            if (!validateChain()) {
                throw new IllegalArgumentException("Invalid chain. Cannot add new block.");
            }
            // Add the new block to the chain
            chain.add(block);

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse block string: " + e.getMessage(), e);
        }
    }

    // Validate the entire chain
    public boolean validateChain() {
        FTCBlock currentBlock;
        FTCBlock previousBlock;

        for (int i = 1; i < chain.size(); i++) {
            currentBlock = chain.get(i);
            previousBlock = chain.get(i - 1);

            // Check if the hash of the current block is valid
            if (!currentBlock.getHash().equals(BlockUtil.calculateFTCBlockHash(currentBlock.getIndex(), currentBlock.getTimestamp(), currentBlock.getFileInfo(), currentBlock.getUserInfos(), currentBlock.getTransactions(), currentBlock.getPrevHash()))) {
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
        for (FTCBlock block : chain) {
            System.out.println("Block #" + block.getIndex());
            System.out.println("Timestamp: " + block.getTimestamp());
            System.out.println("File: " + block.getFileInfo().toString());
            System.out.println("Users: " + block.getUserInfos().toString());
            System.out.println("Transactions: " + block.getTransactions().toString());
            System.out.println("Hash: " + block.getHash());
            System.out.println("Previous Hash: " + block.getPrevHash());
            System.out.println("====================================");
        }
    }

    // Getters
    public List<FTCBlock> getChain() {
        return chain;
    }

    public FTCBlock getBlock(int index) {
        return chain.get(index);
    }

    public FTCBlock getLastBlock() {
        if (chain.isEmpty()) {
            return null; // or throw an exception
        }
        return chain.get(chain.size() - 1);
    }

    // Main method to simulate blockchain usage
    public static void main(String[] args) {
        FTCBlockchain blockchain = new FTCBlockchain();

        // Create a new block and add it to the blockchain
        FileInfo fileInfo = new FileInfo("file.txt", "fileHashCID", "eFileKey");
        List<UserInfo> userInfos = new ArrayList<>();
        userInfos.add(new UserInfo("userPublicKey", "userEncryptedKey"));
        Transaction transaction = new Transaction(null, null, "fileName", "fileHash", "senderPublicKey", "receiverPublicKey", "encryptedFileKey", "upload", "creatorSign", "validatorSign");
        FTCBlock newBlock = new FTCBlock(1, System.currentTimeMillis(), fileInfo, userInfos, transaction, blockchain.getLastBlock().getHash(), BlockUtil.calculateFTCBlockHash(1, System.currentTimeMillis(), fileInfo, userInfos, transaction, blockchain.getLastBlock().getHash()));
        blockchain.addBlock(newBlock);

        System.out.println("Blockchain after adding a new block:");
        blockchain.printBlockchain();

    }
}