package com.intern.trustai.repository;

import com.intern.trustai.entity.HallucinationCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HallucinationCheckRepository extends JpaRepository<HallucinationCheck, Long> {
    List<HallucinationCheck> findByMessageId(Long messageId);
}
