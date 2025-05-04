package models;

import utils.HashUtil;

public class Transaction {
    private String sender;
    private String receiver;
    private String fileHash;
    private String encryptedFileKey;
    private long timestamp;
    private String transactionType;
    private String creatorSign;
    private String validatorSign;

    public Transaction(String sender, String receiver, String fileHash,
                       String encryptedFileKey, long timestamp, String transactionType,
                       String creatorSign, String validatorSign) {
        this.sender = sender;
        this.receiver = receiver;
        this.fileHash = fileHash;
        this.encryptedFileKey = encryptedFileKey;
        this.timestamp = timestamp;
        this.transactionType = transactionType;
        this.creatorSign = creatorSign;
        this.validatorSign = validatorSign;
    }

    public String calculateTransactionHash() {
        String input = sender + receiver + fileHash + encryptedFileKey + timestamp + transactionType + creatorSign + validatorSign;
        return HashUtil.generateSHA256(input);
    }

    @Override
    public String toString() {
        return "{" +
                "\"sender\":\"" + sender + "\"," +
                "\"receiver\":\"" + receiver + "\"," +
                "\"fileHash\":\"" + fileHash + "\"," +
                "\"encryptedFileKey\":\"" + encryptedFileKey + "\"," +
                "\"timestamp\":" + timestamp + "," +
                "\"transactionType\":\"" + transactionType + "\"," +
                "\"creatorSign\":\"" + creatorSign + "\"," +
                "\"validatorSign\":\"" + validatorSign + "\"" +
                "}";
    }

    // Getters and Setters
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }

    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }

    public String getEncryptedFileKey() { return encryptedFileKey; }
    public void setEncryptedFileKey(String encryptedFileKey) { this.encryptedFileKey = encryptedFileKey; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public String getCreatorSign() { return creatorSign; }
    public void setCreatorSign(String creatorSign) { this.creatorSign = creatorSign; }

    public String getValidatorSign() { return validatorSign; }
    public void setValidatorSign(String validatorSign) { this.validatorSign = validatorSign; }
}