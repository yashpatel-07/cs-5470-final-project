package models;

import com.google.gson.Gson;

public class FileInfo {
    private String fileName;
    private String fileHash;
    private String encryptedFileKey; // Encrypted file key for the file

    public FileInfo(String fileName, String fileHash, String encryptedFileKey) {
        this.fileName = fileName;
        this.fileHash = fileHash;
        this.encryptedFileKey = encryptedFileKey;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    // Getters and Setters
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }

    public String getEncryptedFileKey() { return encryptedFileKey; }
    public void setEncryptedFileKey(String encryptedFileKey) { this.encryptedFileKey = encryptedFileKey; }
}