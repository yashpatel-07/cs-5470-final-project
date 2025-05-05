package blockchain;

import models.Transaction;
import models.UserInfo;
import models.FileInfo;
import utils.BlockUtil;

import java.util.ArrayList;
import java.util.List;

public class FTCBlockchain {
    private List<FTCBlock> chain;

    public FTCBlockchain() {
        this.chain = new ArrayList<>();
        createGenesisBlock();
    }

    private void createGenesisBlock() {
        int index = 0;
        long timestamp = 0L;
        String prevHash = "0";
        List<UserInfo> userInfos = new ArrayList<>();
        List<Transaction> transactions = new ArrayList<>();
        userInfos.add(new UserInfo("publicKeyGenesis", "encryptedKey"));
        transactions.add(new Transaction("sender", "receiver", "CID", "fileKey", 0L, "None", "creatorSign", "validatorSign"));
        FileInfo fileInfo = new FileInfo("genesis.txt", "genesisHashCID", System.currentTimeMillis());
        String hash = BlockUtil.calculateFTCBlockHash(index, timestamp, fileInfo, userInfos.get(0), transactions.get(0), prevHash);
        FTCBlock genesisBlock = new FTCBlock(index, timestamp, fileInfo, userInfos, transactions, prevHash, hash);

        // Add the genesis block to the chain
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

    // Validate the entire chain
    public boolean validateChain() {
        FTCBlock currentBlock;
        FTCBlock previousBlock;

        for (int i = 1; i < chain.size(); i++) {
            currentBlock = chain.get(i);
            previousBlock = chain.get(i - 1);

            // Check if the hash of the current block is valid
            if (!currentBlock.getHash().equals(BlockUtil.calculateFTCBlockHash(currentBlock.getIndex(), currentBlock.getTimestamp(), currentBlock.getFileInfo(), currentBlock.getUserInfos().get(0), currentBlock.getTransactions().get(0), currentBlock.getPrevHash()))) {
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
            System.out.println("Transactions: ");
            for (Transaction transaction : block.getTransactions()) {
                System.out.println("\t" + transaction.toString());
            }
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

        // Example 1: Adding a file transaction block
        FileInfo fileInfo1 = new FileInfo("file1.txt", "file1HashCID", System.currentTimeMillis());
        List<UserInfo> userInfos1 = new ArrayList<>();
        userInfos1.add(new UserInfo("user1PublicKey", "user1EncryptedKey"));
        userInfos1.add(new UserInfo("user2PublicKey", "user2EncryptedKey"));
        List<Transaction> transactions1 = new ArrayList<>();
        transactions1.add(new Transaction("user1PublicKey", "user2PublicKey","CID", "fileKey", System.currentTimeMillis(), "share", "creatorSign1", "validatorSign1"));
        FTCBlock block1 = new FTCBlock(1, System.currentTimeMillis(), fileInfo1, userInfos1, transactions1, blockchain.getLastBlock().getHash(), BlockUtil.calculateFTCBlockHash(1, System.currentTimeMillis(), fileInfo1, userInfos1.get(0), transactions1.get(0), blockchain.getLastBlock().getHash()));

        blockchain.addBlock(block1);

        // Example 2: Adding another file transaction block
        FileInfo fileInfo2 = new FileInfo("file2.txt", "file2HashCID", System.currentTimeMillis());
        List<UserInfo> userInfos2 = new ArrayList<>();
        userInfos2.add(new UserInfo("user2PublicKey", "user2EncryptedKey"));
        List<Transaction> transactions2 = new ArrayList<>();
        transactions2.add(new Transaction("user2PublicKey", "user3PublicKey","CID", "fileKey", System.currentTimeMillis(), "share", "creatorSign2", "validatorSign2"));
        FTCBlock block2 = new FTCBlock(2, System.currentTimeMillis(), fileInfo2, userInfos2, transactions2, blockchain.getLastBlock().getHash(), BlockUtil.calculateFTCBlockHash(2, System.currentTimeMillis(), fileInfo2, userInfos2.get(0), transactions2.get(0), blockchain.getLastBlock().getHash()));

        blockchain.addBlock(block2);

        // Print the blockchain to the console
        System.out.println("\nBlockchain Contents:");
        blockchain.printBlockchain();
    }
}