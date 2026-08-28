package com.intern.trustai.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class KafkaProducerService {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String TOPIC_HALLUCINATION = "hallucination-checks";
    private static final String TOPIC_AUDIT = "audit-results";
    private static final String TOPIC_RAG = "rag-interactions";

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    public void sendHallucinationAlert(Long messageId, String userId, int score) {
        Map<String, Object> event = new HashMap<>();
        event.put("messageId", messageId);
        event.put("userId", userId);
        event.put("score", score);
        event.put("timestamp", System.currentTimeMillis());

        kafkaTemplate.send(TOPIC_HALLUCINATION, String.valueOf(messageId), event);
    }

    public void sendAuditCompletedEvent(Long contractId, String contractName, int score, String auditor) {
        Map<String, Object> event = new HashMap<>();
        event.put("contractId", contractId);
        event.put("contractName", contractName);
        event.put("globalRiskScore", score);
        event.put("auditor", auditor);
        event.put("timestamp", System.currentTimeMillis());

        kafkaTemplate.send(TOPIC_AUDIT, String.valueOf(contractId), event);
    }
    
    public void sendRagInteractionEvent(Long messageId, String userId, String query) {
        Map<String, Object> event = new HashMap<>();
        event.put("messageId", messageId);
        event.put("userId", userId);
        event.put("query", query);
        event.put("timestamp", System.currentTimeMillis());

        kafkaTemplate.send(TOPIC_RAG, String.valueOf(messageId), event);
    }
}