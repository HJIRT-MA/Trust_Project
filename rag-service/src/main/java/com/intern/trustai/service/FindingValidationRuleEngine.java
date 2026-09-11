package com.intern.trustai.service;

public interface FindingValidationRuleEngine {

    boolean validateFinding(String category, String codeSnippet);
}
