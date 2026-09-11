package com.intern.trustai.service;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Holds the live SSE emitters for in-progress contract audits, keyed by contract id.
 * Extracted out of AuditController so the controller stays stateless.
 */
@Component
public class AuditStreamRegistry {

    private final ConcurrentMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(Long contractId, long timeoutMillis) {
        SseEmitter emitter = new SseEmitter(timeoutMillis);
        emitters.put(contractId, emitter);
        emitter.onCompletion(() -> emitters.remove(contractId));
        emitter.onTimeout(() -> emitters.remove(contractId));
        return emitter;
    }

    public SseEmitter get(Long contractId) {
        return emitters.get(contractId);
    }

    public void remove(Long contractId) {
        emitters.remove(contractId);
    }
}
