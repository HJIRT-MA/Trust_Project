package com.intern.trustai.controller;

import com.intern.trustai.dto.ContractStructureDTO;
import com.intern.trustai.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "http://localhost:4200")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('admin', 'analyst')")
    public ResponseEntity<ContractStructureDTO> uploadContract(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty() || (!file.getOriginalFilename().endsWith(".sol") && !file.getOriginalFilename().endsWith(".txt"))) {
                return ResponseEntity.badRequest().build();
            }
            ContractStructureDTO result = auditService.uploadAndParseContract(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
