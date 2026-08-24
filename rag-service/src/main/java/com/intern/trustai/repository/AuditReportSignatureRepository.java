package com.intern.trustai.repository;

import com.intern.trustai.entity.AuditReportSignature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuditReportSignatureRepository extends JpaRepository<AuditReportSignature, Long> {
    Optional<AuditReportSignature> findBySmartContractId(Long contractId);
    void deleteBySmartContractId(Long contractId);
}
