package com.intern.trustai.service;

import com.intern.trustai.entity.BlockchainProof;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface BlockchainProofService {
    List<BlockchainProof> getFilteredProofs(String type, String status, String userId);
    Optional<BlockchainProof> getProofById(Integer id);
    Map<String, Object> getDashboardStats();
}
