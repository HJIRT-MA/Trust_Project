package com.intern.trustai.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FindingValidationRuleEngineImplTest {

    private final FindingValidationRuleEngine engine = new FindingValidationRuleEngineImpl();

    @Test
    void reentrancyFindingWithCallValuePatternIsValidated() {
        String code = "function withdraw() public { (bool ok, ) = msg.sender.call{value: amount}(\"\"); }";
        assertTrue(engine.validateFinding("Reentrancy", code));
    }

    @Test
    void reentrancyFindingWithoutCallValuePatternIsRejected() {
        String code = "function withdraw() public { balances[msg.sender] = 0; }";
        assertFalse(engine.validateFinding("Reentrancy", code));
    }

    @Test
    void categoryMatchingIsCaseInsensitive() {
        String code = "msg.sender.call{value: amount}(\"\");";
        assertTrue(engine.validateFinding("reentrancy", code));
    }

    @Test
    void accessControlFindingWithPublicVisibilityIsValidated() {
        String code = "function setOwner(address newOwner) public { owner = newOwner; }";
        assertTrue(engine.validateFinding("Access Control", code));
    }

    @Test
    void unknownCategoryDefaultsToValid() {
        // Categories outside the 8 known SWC buckets have no rule to fire, so the
        // engine trusts the LLM's finding by default instead of silently dropping it.
        String code = "some unrelated code snippet";
        assertTrue(engine.validateFinding("Best Practices", code));
    }

    @Test
    void nullCodeSnippetDefaultsToValid() {
        assertTrue(engine.validateFinding("Reentrancy", null));
    }

    @Test
    void nullCategoryDefaultsToValid() {
        assertTrue(engine.validateFinding(null, "some code"));
    }
}
