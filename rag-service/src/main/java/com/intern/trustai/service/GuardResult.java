package com.intern.trustai.service;

public class GuardResult {
    private int confidenceScore;
    private String claimAnalysis;

    public int getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(int confidenceScore) { this.confidenceScore = confidenceScore; }
    public String getClaimAnalysis() { return claimAnalysis; }
    public void setClaimAnalysis(String claimAnalysis) { this.claimAnalysis = claimAnalysis; }
}
