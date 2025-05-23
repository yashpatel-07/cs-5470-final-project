package models;

import com.google.gson.Gson;

public class UserInfo {
    private String publicKey; // User's public key
    private String encryptedFileKey; // Encrypted file key for the user

    public UserInfo(String publicKey, String encryptedFileKey) {
        this.publicKey = publicKey;
        this.encryptedFileKey = encryptedFileKey;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    // Getters and Setters
    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

    public String getEncryptedFileKey() { return encryptedFileKey; }
    public void setEncryptedFileKey(String encryptedFileKey) { this.encryptedFileKey = encryptedFileKey; }
}