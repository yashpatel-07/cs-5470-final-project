package models;

import com.google.gson.Gson;
import utils.HashUtil;

public class Transaction {
    private NodeInfo sender;
    private NodeInfo receiver;
    private String fileName;
    private String fileHash;
    private String senderPublicKey;
    private String receiverPublicKey;
    private String encryptedFileKey;
    private String transactionType;
    private String creatorSign;
    private String validatorSign;

    public Transaction(NodeInfo sender, NodeInfo receiver, String fileName, String fileHash, String senderPublicKey, String receiverPublicKey,
                       String encryptedFileKey, String transactionType,
                       String creatorSign, String validatorSign) {
        this.sender = sender;
        this.receiver = receiver;
        this.fileName = fileName;
        this.fileHash = fileHash;
        this.senderPublicKey = senderPublicKey;
        this.receiverPublicKey = receiverPublicKey;
        this.encryptedFileKey = encryptedFileKey;
        this.transactionType = transactionType;
        this.creatorSign = creatorSign;
        this.validatorSign = validatorSign;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }


    // Getters and Setters
    public NodeInfo getSender() { return sender; }
    public void setSender(NodeInfo sender) { this.sender = sender; }

    public NodeInfo getReceiver() { return receiver; }
    public void setReceiver(NodeInfo receiver) { this.receiver = receiver; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getSenderPublicKey() { return senderPublicKey; }
    public void setSenderPublicKey(String senderPublicKey) { this.senderPublicKey = senderPublicKey; }

    public String getReceiverPublicKey() { return receiverPublicKey; }
    public void setReceiverPublicKey(String receiverPublicKey) { this.receiverPublicKey = receiverPublicKey; }

    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }

    public String getEncryptedFileKey() { return encryptedFileKey; }
    public void setEncryptedFileKey(String encryptedFileKey) { this.encryptedFileKey = encryptedFileKey; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public String getCreatorSign() { return creatorSign; }
    public void setCreatorSign(String creatorSign) { this.creatorSign = creatorSign; }

    public String getValidatorSign() { return validatorSign; }
    public void setValidatorSign(String validatorSign) { this.validatorSign = validatorSign; }
}