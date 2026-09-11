package com.intern.trustai.controller;

import com.intern.trustai.dto.AuditFindingDTO;
import com.intern.trustai.dto.ContractStructureDTO;
import com.intern.trustai.dto.SmartContractDTO;
import com.intern.trustai.security.TenantContext;
import com.intern.trustai.service.AuditOrchestrationService;
import com.intern.trustai.service.AuditService;
import com.intern.trustai.service.AuditStreamRegistry;
import com.intern.trustai.service.SecurityAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.intern.trustai.service.SecurityPdfReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private static final Logger log = LoggerFactory.getLogger(AuditController.class);

    private final AuditService auditService;
    private final SecurityAuditService securityAuditService;
    private final SecurityPdfReportService pdfReportService;
    private final AuditStreamRegistry streamRegistry;
    private final AuditOrchestrationService auditOrchestrationService;

    public AuditController(AuditService auditService,
                           SecurityAuditService securityAuditService,
                           SecurityPdfReportService pdfReportService,
                           AuditStreamRegistry streamRegistry,
                           AuditOrchestrationService auditOrchestrationService) {
        this.auditService = auditService;
        this.securityAuditService = securityAuditService;
        this.pdfReportService = pdfReportService;
        this.streamRegistry = streamRegistry;
        this.auditOrchestrationService = auditOrchestrationService;
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
            log.error("Failed to upload/parse contract", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(value = "/stream/{contractId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('admin', 'analyst')")
    public SseEmitter streamAudit(@PathVariable("contractId") Long contractId) {
        SseEmitter emitter = streamRegistry.register(contractId, 600_000L); // 10 minutes
        try {
            emitter.send("connected");
        } catch (Exception e) {
            log.warn("Could not send initial SSE event for contract {}", contractId, e);
        }
        return emitter;
    }

    @PostMapping("/{contractId}/analyze")
    @PreAuthorize("hasAnyRole('admin', 'analyst')")
    public ResponseEntity<String> startAnalysis(@PathVariable("contractId") Long contractId, Authentication authentication) {
        String currentTenant = TenantContext.getCurrentTenant();
        String auditor = authentication != null ? authentication.getName() : "Unknown";

        auditOrchestrationService.runAnalysisAsync(contractId, currentTenant, auditor);
        return ResponseEntity.accepted().body("{\"status\": \"started\"}");
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('admin', 'analyst', 'viewer')")
    public ResponseEntity<List<SmartContractDTO>> getHistory() {
        List<SmartContractDTO> history = auditService.getAllContracts().stream()
                .map(SmartContractDTO::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{contractId}/findings")
    @PreAuthorize("hasAnyRole('admin', 'analyst', 'viewer')")
    public ResponseEntity<List<AuditFindingDTO>> getFindings(@PathVariable("contractId") Long contractId) {
        List<AuditFindingDTO> findings = auditService.getFindingsByContractId(contractId).stream()
                .map(AuditFindingDTO::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(findings);
    }

    @GetMapping("/{contractId}/report/pdf")
    @PreAuthorize("hasAnyRole('admin', 'analyst', 'viewer')")
    public ResponseEntity<byte[]> downloadPdfReport(@PathVariable("contractId") Long contractId) {
        try {
            byte[] pdf = pdfReportService.generateAndSignReport(contractId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", "Security_Audit_Report_" + contractId + ".pdf");
            return ResponseEntity.ok().headers(headers).body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate PDF report for contract {}", contractId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{contractId}")
    @PreAuthorize("hasAnyRole('admin', 'analyst')")
    public ResponseEntity<Void> deleteAudit(@PathVariable("contractId") Long contractId) {
        auditService.deleteAudit(contractId);
        return ResponseEntity.noContent().build();
    }

    // Access-denied and uncaught-exception handling now live in GlobalExceptionHandler.
}
