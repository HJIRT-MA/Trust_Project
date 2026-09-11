package com.intern.trustai.service;

public interface BlockchainService {

    String storeAuditProof(String reportContent, String metadata) throws Exception;

    boolean verifyAuditProof(String reportContent) throws Exception;
}
