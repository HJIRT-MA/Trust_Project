package com.intern.trustai.repository;

import com.intern.trustai.entity.SmartContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SmartContractRepository extends JpaRepository<SmartContract, Long> {
}
