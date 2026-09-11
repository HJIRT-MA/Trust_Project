package com.intern.trustai.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void returnsNullWhenNoTenantWasSet() {
        assertNull(TenantContext.getCurrentTenant());
    }

    @Test
    void returnsTheTenantThatWasSetOnTheCurrentThread() {
        TenantContext.setCurrentTenant("acme-corp");
        assertEquals("acme-corp", TenantContext.getCurrentTenant());
    }

    @Test
    void clearRemovesTheTenantForTheCurrentThread() {
        TenantContext.setCurrentTenant("acme-corp");
        TenantContext.clear();
        assertNull(TenantContext.getCurrentTenant());
    }

    @Test
    void tenantIsNotVisibleFromAnotherThread() throws InterruptedException {
        TenantContext.setCurrentTenant("acme-corp");

        String[] otherThreadValue = new String[1];
        Thread other = new Thread(() -> otherThreadValue[0] = TenantContext.getCurrentTenant());
        other.start();
        other.join();

        assertNull(otherThreadValue[0], "TenantContext must not leak across threads");
        assertEquals("acme-corp", TenantContext.getCurrentTenant());
    }
}
