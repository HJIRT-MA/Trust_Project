package com.intern.trustai.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class KafkaProducerService {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "Hallucination-alerts";
    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    public void sendHallucinationAlert(Long messageId, String userId, int score) {
        Map<String, Object> event = new HashMap<>();
        event.put("messageId", messageId);
        event.put("userId", userId);
        event.put("score", score);
        event.put("timestamp", System.currentTimeMillis());

        kafkaTemplate.send(TOPIC, String.valueOf(messageId), event);
    }
}