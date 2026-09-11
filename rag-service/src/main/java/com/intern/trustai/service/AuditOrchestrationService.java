package com.intern.trustai.service;

import com.intern.trustai.entity.AuditFinding;
import com.intern.trustai.security.TenantContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Runs a contract security audit off the request thread. Previously this orchestration
 * (background thread + tenant propagation + SSE completion) lived directly inside
 * AuditController; it now belongs to the service layer, with Spring's task executor
 * standing in for the ad-hoc `new Thread(...)`.
 */
@Service
public class AuditOrchestrationService {

    private final SecurityAuditService securityAuditService;
    private final AuditStreamRegistry streamRegistry;

    public AuditOrchestrationService(SecurityAuditService securityAuditService, AuditStreamRegistry streamRegistry) {
        this.securityAuditService = securityAuditService;
        this.streamRegistry = streamRegistry;
    }

    @Async
    public void runAnalysisAsync(Long contractId, String tenantId, String auditor) {
        TenantContext.setCurrentTenant(tenantId);
        SseEmitter emitter = null;
        try {
            emitter = awaitEmitter(contractId);
            sendSafely(emitter, "Starting security audit...");

            List<AuditFinding> findings = securityAuditService.runSecurityAudit(contractId, emitter, auditor);

            if (emitter != null) {
                emitter.send(SseEmitter.event().name("complete").data(findings));
                emitter.complete();
            }
        } catch (Exception e) {
            if (emitter != null) {
                emitter.completeWithError(e);
            }
        } finally {
            TenantContext.clear();
        }
    }

    private SseEmitter awaitEmitter(Long contractId) throws InterruptedException {
        SseEmitter emitter = null;
        for (int i = 0; i < 10; i++) {
            emitter = streamRegistry.get(contractId);
            if (emitter != null) break;
            Thread.sleep(500);
        }
        return emitter;
    }

    private void sendSafely(SseEmitter emitter, String message) {
        if (emitter == null) return;
        try {
            emitter.send(message);
        } catch (Exception ignored) {
            // client likely disconnected
        }
    }
}
