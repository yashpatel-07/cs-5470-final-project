package models;

import com.google.gson.Gson;

public class VoteInfo {
    private String voterId;
    private String candidateId;
    private double voteWeight;

    public VoteInfo(String voterId, String candidateId, double voteWeight) {
        this.voterId = voterId;
        this.candidateId = candidateId;
        this.voteWeight = voteWeight;
    }

//    @Override
//    public String toString() {
//        return "{" +
//                "\"voterId\":\"" + voterId + "\"," +
//                "\"candidateId\":\"" + candidateId + "\"," +
//                "\"voteWeight\":" + voteWeight +
//                "}";
//    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    // Getters and Setters
    public String getVoterId() { return voterId; }
    public void setVoterId(String voterId) { this.voterId = voterId; }

    public String getCandidateId() { return candidateId; }
    public void setCandidateId(String candidateId) { this.candidateId = candidateId; }

    public double getVoteWeight() { return voteWeight; }
    public void setVoteWeight(double voteWeight) { this.voteWeight = voteWeight; }
}