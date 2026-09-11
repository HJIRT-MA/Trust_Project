package com.intern.trustai.service;

public interface HallucinationGuardService {

    GuardResult verifyClaims(String aiResponse, Long messageId, String tenantId);
}
