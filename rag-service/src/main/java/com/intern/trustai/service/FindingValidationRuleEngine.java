package com.intern.trustai.service;

import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.api.RulesEngine;
import org.jeasy.rules.core.DefaultRulesEngine;
import org.jeasy.rules.core.RuleBuilder;
import org.springframework.stereotype.Service;

@Service
public class FindingValidationRuleEngine {

    private final RulesEngine rulesEngine;
    private final Rules rules;

    public FindingValidationRuleEngine() {
        this.rulesEngine = new DefaultRulesEngine();
        this.rules = new Rules();
        registerRules();
    }

    private void registerRules() {
        rules.register(new RuleBuilder()
                .name("Reentrancy Validator")
                .when(facts -> "Reentrancy".equalsIgnoreCase(facts.get("category")) && 
                               ((String)facts.get("code")).contains(".call{value:"))
                .then(facts -> facts.put("isValid", true))
                .build());

        rules.register(new RuleBuilder()
                .name("TxOrigin Validator")
                .when(facts -> "Authorization".equalsIgnoreCase(facts.get("category")) && 
                               ((String)facts.get("code")).contains("tx.origin"))
                .then(facts -> facts.put("isValid", true))
                .build());

        rules.register(new RuleBuilder()
                .name("Timestamp Validator")
                .when(facts -> "Timestamp".equalsIgnoreCase(facts.get("category")) && 
                               ((String)facts.get("code")).contains("block.timestamp"))
                .then(facts -> facts.put("isValid", true))
                .build());

        rules.register(new RuleBuilder()
                .name("Selfdestruct Validator")
                .when(facts -> "Denial of Service".equalsIgnoreCase(facts.get("category")) && 
                               ((String)facts.get("code")).contains("selfdestruct"))
                .then(facts -> facts.put("isValid", true))
                .build());

        rules.register(new RuleBuilder()
                .name("Delegatecall Validator")
                .when(facts -> "Injection".equalsIgnoreCase(facts.get("category")) && 
                               ((String)facts.get("code")).contains("delegatecall"))
                .then(facts -> facts.put("isValid", true))
                .build());

        rules.register(new RuleBuilder()
                .name("Floating Pragma Validator")
                .when(facts -> "Compiler".equalsIgnoreCase(facts.get("category")) && 
                               ((String)facts.get("code")).contains("pragma solidity ^"))
                .then(facts -> facts.put("isValid", true))
                .build());

        rules.register(new RuleBuilder()
                .name("Unchecked Math Validator")
                .when(facts -> "Arithmetic".equalsIgnoreCase(facts.get("category")) && 
                               ((String)facts.get("code")).contains("unchecked"))
                .then(facts -> facts.put("isValid", true))
                .build());

        rules.register(new RuleBuilder()
                .name("Visibility Validator")
                .when(facts -> "Access Control".equalsIgnoreCase(facts.get("category")) && 
                               (((String)facts.get("code")).contains("public") || ((String)facts.get("code")).contains("external")))
                .then(facts -> facts.put("isValid", true))
                .build());
    }

    public boolean validateFinding(String category, String codeSnippet) {
        if (codeSnippet == null || category == null) return true; 
        
        boolean isStrictCategory = category.equalsIgnoreCase("Reentrancy") ||
                                   category.equalsIgnoreCase("Authorization") ||
                                   category.equalsIgnoreCase("Timestamp") ||
                                   category.equalsIgnoreCase("Denial of Service") ||
                                   category.equalsIgnoreCase("Injection") ||
                                   category.equalsIgnoreCase("Compiler") ||
                                   category.equalsIgnoreCase("Arithmetic") ||
                                   category.equalsIgnoreCase("Access Control");

        Facts facts = new Facts();
        facts.put("category", category);
        facts.put("code", codeSnippet);
        facts.put("isValid", !isStrictCategory); 

        rulesEngine.fire(rules, facts);

        return facts.get("isValid");
    }
}
