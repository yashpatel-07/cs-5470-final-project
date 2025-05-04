package models;

public class FileInfo {
    private String fileName;
    private String fileHash;
    private long timestamp;

    public FileInfo(String fileName, String fileHash, long timestamp) {
        this.fileName = fileName;
        this.fileHash = fileHash;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "{" +
                "\"fileName\":\"" + fileName + "\"," +
                "\"fileHash\":\"" + fileHash + "\"," +
                "\"timestamp\":" + timestamp +
                "}";
    }

    // Getters and Setters
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}