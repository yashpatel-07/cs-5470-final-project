package blockchain;

import models.Transaction;
import models.UserInfo;
import models.FileInfo;

import java.util.ArrayList;
import java.util.List;

public class FTCBlockchain {
    private List<FTCBlock> chain;

    public FTCBlockchain() {
        this.chain = new ArrayList<>();
        createGenesisBlock();
    }

    private void createGenesisBlock() {
        FileInfo genesisFile = new FileInfo("genesis.txt", "genesisHashCID", System.currentTimeMillis());
        List<UserInfo> users = new ArrayList<>();
        users.add(new UserInfo("publicKeyGenesis", "encryptedKey"));
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(new Transaction("sender", "receiver", "CID", "fileKey",System.currentTimeMillis(), "None", "creatorSign", "validatorSign"));
        FTCBlock genesisBlock = new FTCBlock(0, genesisFile, users, transactions, "0");
        this.chain.add(genesisBlock);
    }

    public void addBlock(FileInfo fileInfo, List<UserInfo> userInfos, List<Transaction> transactions) {
        FTCBlock lastBlock = chain.get(chain.size() - 1);
        FTCBlock newBlock = new FTCBlock(chain.size(), fileInfo, userInfos, transactions, lastBlock.getHash());
        // Validate the chain before adding the new block
        if (!validateChain()) {
            System.out.println("Invalid chain. Cannot add new block.");
            return;
        }
        chain.add(newBlock);
    }

    // Validate the entire chain
    public boolean validateChain() {
        FTCBlock currentBlock;
        FTCBlock previousBlock;

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

        blockchain.addBlock(fileInfo1, userInfos1, transactions1);

        // Example 2: Adding another file transaction block
        FileInfo fileInfo2 = new FileInfo("file2.txt", "file2HashCID", System.currentTimeMillis());
        List<UserInfo> userInfos2 = new ArrayList<>();
        userInfos2.add(new UserInfo("user2PublicKey", "user2EncryptedKey"));
        List<Transaction> transactions2 = new ArrayList<>();
        transactions2.add(new Transaction("user2PublicKey", "user3PublicKey","CID", "fileKey", System.currentTimeMillis(), "share", "creatorSign2", "validatorSign2"));

        blockchain.addBlock(fileInfo2, userInfos2, transactions2);

        // Print the blockchain to the console
        System.out.println("\nBlockchain Contents:");
        blockchain.printBlockchain();
    }
}