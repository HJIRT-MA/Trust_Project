package com.intern.trustai.service;

public interface KafkaProducerService {

    void sendHallucinationAlert(Long messageId, String userId, int score);

    void sendAuditCompletedEvent(Long contractId, String contractName, int score, String auditor);

    void sendRagInteractionEvent(Long messageId, String userId, String query);

    void sendSecurityAlert(Integer proofId, String alertMessage);
}
