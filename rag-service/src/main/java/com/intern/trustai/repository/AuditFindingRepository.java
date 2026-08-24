package com.intern.trustai.repository;

import com.intern.trustai.entity.AuditFinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditFindingRepository extends JpaRepository<AuditFinding, Long> {
    List<AuditFinding> findBySmartContractId(Long smartContractId);
    void deleteBySmartContractId(Long smartContractId);
}
