package blockchain;

import com.google.gson.Gson;
import models.NodeInfo;
import models.VoteInfo;

import java.util.List;

public class FICBlock {
    private final int index;
    private final long timestamp;
    private final String prevHash;
    private final List<List<NodeInfo>> nodeInfos; // Information about nodes (efficiency, reputation)
    private final List<VoteInfo> voteInfos; // Voting records for leadership selection
    private final String merkleRoot; // Root of the Merkle tree of transactions
    private final String hash;

    public FICBlock(int index, long timestamp, List<List<NodeInfo>> nodeInfos, List<VoteInfo> voteInfos, String prevHash, String merkleRoot, String hash) {
        this.index = index;
        this.timestamp = timestamp;
        this.prevHash = prevHash;
        this.nodeInfos = nodeInfos;
        this.voteInfos = voteInfos;
        this.merkleRoot = merkleRoot;
        this.hash = hash;
    }

    // Getters
    public int getIndex() { return index;}

    public long getTimestamp() {
        return timestamp;
    }

    public String getPrevHash() {
        return prevHash;
    }

    public List<List<NodeInfo>> getNodeInfos() {
        return nodeInfos;
    }

    public List<VoteInfo> getVoteInfos() {
        return voteInfos;
    }

    public String getMerkleRoot() {
        return merkleRoot;
    }

    public String getHash() { return hash; }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}