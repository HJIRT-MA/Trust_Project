package com.intern.trustai.service;

import com.intern.trustai.dto.ContractStructureDTO;
import com.intern.trustai.entity.SmartContract;
import com.intern.trustai.entity.AuditFinding;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface AuditService {
    List<SmartContract> getAllContracts();
    List<AuditFinding> getFindingsByContractId(Long contractId);
    void deleteAudit(Long contractId);
    ContractStructureDTO uploadAndParseContract(MultipartFile file) throws IOException;
}
