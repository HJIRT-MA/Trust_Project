package com.intern.trustai.service;

import com.intern.trustai.entity.BlockchainProof;
import com.intern.trustai.repository.BlockchainProofRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class BlockchainProofServiceImpl implements BlockchainProofService {

    private final BlockchainProofRepository proofRepository;

    public BlockchainProofServiceImpl(BlockchainProofRepository proofRepository) {
        this.proofRepository = proofRepository;
    }

    public List<BlockchainProof> getFilteredProofs(String type, String status, String userId) {
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

        // Tri par date décroissante
        return proofs.stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .toList();
    }

    public Optional<BlockchainProof> getProofById(Integer id) {
        return proofRepository.findById(id);
    }

    public Map<String, Object> getDashboardStats() {
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

        return stats;
    }
}
