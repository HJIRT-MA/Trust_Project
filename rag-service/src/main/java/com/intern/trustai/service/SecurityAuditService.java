package com.intern.trustai.service;

import com.intern.trustai.entity.AuditFinding;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface SecurityAuditService {

    List<AuditFinding> runSecurityAudit(Long contractId, SseEmitter emitter, String auditor);
}
