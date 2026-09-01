package com.intern.trustai.controller;

import com.intern.trustai.dto.ContractStructureDTO;
import com.intern.trustai.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.intern.trustai.entity.AuditFinding;
import com.intern.trustai.entity.SmartContract;
import com.intern.trustai.repository.AuditFindingRepository;
import com.intern.trustai.repository.SmartContractRepository;
import com.intern.trustai.service.SecurityPdfReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "http://localhost:4200")
public class AuditController {

    private final AuditService auditService;
    private final com.intern.trustai.service.SecurityAuditService securityAuditService;
    private final SecurityPdfReportService pdfReportService;
    private final java.util.Map<Long, org.springframework.web.servlet.mvc.method.annotation.SseEmitter> emitters = new java.util.concurrent.ConcurrentHashMap<>();

    public AuditController(AuditService auditService, 
                           com.intern.trustai.service.SecurityAuditService securityAuditService,
                           SecurityPdfReportService pdfReportService) {
        this.auditService = auditService;
        this.securityAuditService = securityAuditService;
        this.pdfReportService = pdfReportService;
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

    @GetMapping(value = "/stream/{contractId}", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamAudit(@PathVariable("contractId") Long contractId) {
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(600000L); // 10 minutes
        emitters.put(contractId, emitter);
        try {
            emitter.send("connected");
        } catch (Exception e) {}
        emitter.onCompletion(() -> emitters.remove(contractId));
        emitter.onTimeout(() -> emitters.remove(contractId));
        return emitter;
    }

    @PostMapping("/{contractId}/analyze")
    @PreAuthorize("hasAnyRole('admin', 'analyst')")
    public ResponseEntity<String> startAnalysis(@PathVariable("contractId") Long contractId, Authentication authentication) {
        String currentTenant = com.intern.trustai.security.TenantContext.getCurrentTenant();
        String auditor = authentication != null ? authentication.getName() : "Unknown";
        
        try {
            new Thread(() -> {
                com.intern.trustai.security.TenantContext.setCurrentTenant(currentTenant);
                try {
                    org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = null;
                    for (int i = 0; i < 10; i++) {
                        emitter = emitters.get(contractId);
                        if (emitter != null) break;
                        Thread.sleep(500);
                    }
                    if (emitter != null) emitter.send("Starting security audit...");
                    
                    java.util.List<com.intern.trustai.entity.AuditFinding> findings = securityAuditService.runSecurityAudit(contractId, emitter, auditor);
                    
                    if (emitter != null) {
                        emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().name("complete").data(findings));
                        emitter.complete();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = emitters.get(contractId);
                    if (emitter != null) emitter.completeWithError(e);
                } finally {
                    com.intern.trustai.security.TenantContext.clear();
                }
            }).start();
            return ResponseEntity.accepted().body("{\"status\": \"started\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('admin', 'analyst', 'viewer')")
    public ResponseEntity<java.util.List<SmartContract>> getHistory() {
        return ResponseEntity.ok(auditService.getAllContracts());
    }

    @GetMapping("/{contractId}/findings")
    @PreAuthorize("hasAnyRole('admin', 'analyst', 'viewer')")
    public ResponseEntity<java.util.List<AuditFinding>> getFindings(@PathVariable("contractId") Long contractId) {
        return ResponseEntity.ok(auditService.getFindingsByContractId(contractId));
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
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{contractId}")
    @PreAuthorize("hasAnyRole('admin', 'analyst')")
    public ResponseEntity<Void> deleteAudit(@PathVariable("contractId") Long contractId) {
        auditService.deleteAudit(contractId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<String> handleThrowable(Throwable ex) {
        ex.printStackTrace();
        return ResponseEntity.internalServerError().body("SERVER CRASH: " + ex.getClass().getName() + " - " + ex.getMessage());
    }
}
