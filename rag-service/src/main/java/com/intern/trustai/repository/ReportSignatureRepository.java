package com.intern.trustai.repository;

import com.intern.trustai.entity.ReportSignature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ReportSignatureRepository extends JpaRepository<ReportSignature, Long> {
    Optional<ReportSignature> findByMessageId(Long messageId);
    List<ReportSignature> findAllByOrderByCreatedAtDesc();
}
