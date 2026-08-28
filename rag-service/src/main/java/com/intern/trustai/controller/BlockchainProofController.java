package com.intern.trustai.controller;

import com.intern.trustai.entity.BlockchainProof;
import com.intern.trustai.repository.BlockchainProofRepository;
import com.intern.trustai.service.BlockchainService;
import com.intern.trustai.service.KafkaProducerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/proofs")
public class BlockchainProofController {

    private final BlockchainProofRepository proofRepository;
    private final BlockchainService blockchainService;
    private final KafkaProducerService kafkaProducerService; // Assuming this exists or using a generic one for alerts

    public BlockchainProofController(BlockchainProofRepository proofRepository, 
                                     BlockchainService blockchainService,
                                     KafkaProducerService kafkaProducerService) {
        this.proofRepository = proofRepository;
        this.blockchainService = blockchainService;
        this.kafkaProducerService = kafkaProducerService;
    }

    @GetMapping
    public ResponseEntity<List<BlockchainProof>> getProofs(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userId) {
        
        List<BlockchainProof> proofs;
        if (type != null && !type.isEmpty()) {
            proofs = proofRepository.findByEventType(type);
        } else if (status != null && !status.isEmpty()) {
            proofs = proofRepository.findByStatus(status);
        } else if (userId != null && !userId.isEmpty()) {
            proofs = proofRepository.findByUserId(userId);
        } else {
            proofs = proofRepository.findAll();
        }
        
        proofs.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        return ResponseEntity.ok(proofs);
    }

    @GetMapping("/verify/{id}")
    public ResponseEntity<Map<String, Object>> verifyProof(@PathVariable Integer id) {
        BlockchainProof proof = proofRepository.findById(id).orElse(null);
        if (proof == null) return ResponseEntity.notFound().build();

        Map<String, Object> response = new HashMap<>();
        try {
            boolean isValid = blockchainService.verifyAuditProof(proof.getPayload());
            response.put("valid", isValid);
            
            if (!isValid) {
                // Si altéré, on peut générer une alerte Kafka. 
                // Pour l'instant, on se contente de renvoyer valid: false au front
                // kafkaProducerService.sendSecurityAlert(...);
            }
        } catch (Exception e) {
            response.put("valid", false);
            response.put("error", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        List<BlockchainProof> proofs = proofRepository.findAll();
        
        long total = proofs.size();
        long confirmed = proofs.stream().filter(p -> "CONFIRMED".equals(p.getStatus())).count();
        
        Map<String, Long> byType = new HashMap<>();
        for (BlockchainProof p : proofs) {
            byType.put(p.getEventType(), byType.getOrDefault(p.getEventType(), 0L) + 1);
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("confirmed", confirmed);
        stats.put("confirmationRate", total > 0 ? (double) confirmed / total * 100 : 0);
        stats.put("byType", byType);
        
        // Latest audits (only audit-results)
        List<BlockchainProof> recentAudits = proofs.stream()
            .filter(p -> "audit-results".equals(p.getEventType()))
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(5)
            .toList();
            
        stats.put("recentAudits", recentAudits);

        return ResponseEntity.ok(stats);
    }
}
