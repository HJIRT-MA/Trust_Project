package com.intern.trustai.repository;

import com.intern.trustai.entity.BlockchainProof;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlockchainProofRepository extends JpaRepository<BlockchainProof, Integer> {
    List<BlockchainProof> findByStatus(String status);
    List<BlockchainProof> findByEventType(String eventType);
    List<BlockchainProof> findByUserId(String userId);
}
