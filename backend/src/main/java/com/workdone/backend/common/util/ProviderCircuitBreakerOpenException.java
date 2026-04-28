package com.workdone.backend.common.util;

import java.time.Instant;

public class ProviderCircuitBreakerOpenException extends RuntimeException {
    public ProviderCircuitBreakerOpenException(String providerName, Instant openUntil) {
        super("Circuit breaker OPEN for provider " + providerName + " until " + openUntil);
    }
}