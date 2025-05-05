package blockchain;

import models.FileInfo;
import models.Transaction;
import models.UserInfo;
import utils.HashUtil;

import java.util.List;

public class FTCBlock {
    private final int index;
    private final long timestamp;
    private final FileInfo fileInfo; // Details about the file
    private final List<UserInfo> userInfos; // List of users associated with the file
    private final List<Transaction> transactions;
    private final String prevHash;
    private final String hash;

    public FTCBlock(int index, long timestamp, FileInfo fileInfo, List<UserInfo> userInfos, List<Transaction> transactions, String prevHash, String hash) {
        this.index = index;
        this.timestamp = timestamp;
        this.fileInfo = fileInfo;
        this.userInfos = userInfos;
        this.transactions = transactions;
        this.prevHash = prevHash;
        this.hash = hash;
    }

    // getters
    public int getIndex() {
        return index;
    }

    public  String getPrevHash() {
        return  prevHash;
    }

    public String getHash() {
        return hash;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public List<UserInfo> getUserInfos() {
        return userInfos;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

}
