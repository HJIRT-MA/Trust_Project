package com.intern.trustai.controller;

import com.intern.trustai.entity.BlockchainProof;
import com.intern.trustai.service.BlockchainProofService;
import com.intern.trustai.service.BlockchainService;
import com.intern.trustai.service.KafkaProducerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/proofs")
@CrossOrigin(origins = "http://localhost:4200")
@PreAuthorize("hasAnyRole('admin', 'analyst', 'viewer')")
public class BlockchainProofController {

    private final BlockchainProofService proofService;
    private final BlockchainService blockchainService;
    private final KafkaProducerService kafkaProducerService;
    private final com.intern.trustai.service.AiActComplianceReportService aiActComplianceReportService;

    public BlockchainProofController(BlockchainProofService proofService, 
                                     BlockchainService blockchainService,
                                     KafkaProducerService kafkaProducerService,
                                     com.intern.trustai.service.AiActComplianceReportService aiActComplianceReportService) {
        this.proofService = proofService;
        this.blockchainService = blockchainService;
        this.kafkaProducerService = kafkaProducerService;
        this.aiActComplianceReportService = aiActComplianceReportService;
    }

    @GetMapping
    public ResponseEntity<?> getProofs(
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "userId", required = false) String userId) {
        try {
            return ResponseEntity.ok(proofService.getFilteredProofs(type, status, userId));
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            return ResponseEntity.status(500).body("Error: " + e.getMessage() + "\n" + sw.toString());
        }
    }

    @GetMapping("/verify/{id}")
    public ResponseEntity<Map<String, Object>> verifyProof(@PathVariable Integer id) {
        BlockchainProof proof = proofService.getProofById(id).orElse(null);
        if (proof == null) return ResponseEntity.notFound().build();

        Map<String, Object> response = new HashMap<>();
        try {
            boolean isValid = blockchainService.verifyAuditProof(proof.getPayload());
            response.put("valid", isValid);

            if (!isValid) {
                kafkaProducerService.sendSecurityAlert(id, "Blockchain verification failed! Payload hash does not match on-chain record.");
            }
        } catch (Exception e) {
            response.put("valid", false);
            response.put("error", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(proofService.getDashboardStats());
    }

    @GetMapping("/compliance-report/pdf")
    public ResponseEntity<byte[]> downloadComplianceReport() {
        try {
            byte[] pdf = aiActComplianceReportService.generateComplianceReportPdf();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", "Rapport_Conformite_AI_Act.pdf");
            return ResponseEntity.ok().headers(headers).body(pdf);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
