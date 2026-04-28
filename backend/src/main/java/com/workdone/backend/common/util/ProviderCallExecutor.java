package com.workdone.backend.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Callable;

@Slf4j
@Component
public class ProviderCallExecutor {

    private static final int MAX_ATTEMPTS = 3;
    private static final long BACKOFF_MS = 350L;
    private static final int FAILURE_THRESHOLD = 4;
    private static final Duration CIRCUIT_OPEN_DURATION = Duration.ofSeconds(45);

    private final ProviderErrorMetrics providerErrorMetrics;
    private final ConcurrentHashMap<String, CircuitState> circuits = new ConcurrentHashMap<>();

    public ProviderCallExecutor(ProviderErrorMetrics providerErrorMetrics) {
        this.providerErrorMetrics = providerErrorMetrics;
    }

    public <T> T execute(String providerName, Callable<T> action) throws Exception {
        CircuitState state = circuits.computeIfAbsent(providerName, ignored -> new CircuitState());
        if (state.isOpen()) {
            providerErrorMetrics.increment(providerName, ProviderErrorType.CIRCUIT_OPEN);
            throw new ProviderCircuitBreakerOpenException(providerName, state.openUntil());
        }

        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                T result = action.call();
                state.onSuccess();
                return result;
            } catch (Exception ex) {
                lastException = ex;
                providerErrorMetrics.increment(providerName, classify(ex));
                boolean retryable = isRetryable(ex);
                if (!retryable || attempt == MAX_ATTEMPTS) {
                    state.onFailure();
                    throw ex;
                }
                log.warn("⏱️ [{}] Próba {}/{} nieudana ({}). Retry za {} ms...",
                        providerName, attempt, MAX_ATTEMPTS, ex.getClass().getSimpleName(), BACKOFF_MS);
                sleepBackoff();
            }
        }

        state.onFailure();
        throw lastException;
    }

    private boolean isRetryable(Exception ex) {
        if (ex instanceof ResourceAccessException) {
            return true;
        }
        String message = ex.getMessage();
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("timeout") || normalized.contains("timed out")
                || normalized.contains("i/o error") || normalized.contains("connection reset");
    }

    private void sleepBackoff() {
        try {
            Thread.sleep(BACKOFF_MS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private ProviderErrorType classify(Exception ex) {
        if (isTimeout(ex)) {
            return ProviderErrorType.TIMEOUT;
        }
        if (ex instanceof RestClientResponseException responseException) {
            int status = responseException.getStatusCode().value();
            if (status >= 500) {
                return ProviderErrorType.HTTP_5XX;
            }
            if (status >= 400) {
                return ProviderErrorType.HTTP_4XX;
            }
        }
        return ProviderErrorType.OTHER;
    }

    private boolean isTimeout(Exception ex) {
        if (ex instanceof ResourceAccessException) {
            return true;
        }
        String message = ex.getMessage();
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("timeout") || normalized.contains("timed out")
                || normalized.contains("i/o error") || normalized.contains("connection reset");
    }

    private final class CircuitState {
        private int consecutiveFailures = 0;
        private Instant openUntil;

        synchronized boolean isOpen() {
            if (openUntil == null) {
                return false;
            }
            if (Instant.now().isAfter(openUntil)) {
                openUntil = null;
                consecutiveFailures = 0;
                return false;
            }
            return true;
        }

        synchronized void onSuccess() {
            consecutiveFailures = 0;
            openUntil = null;
        }

        synchronized void onFailure() {
            consecutiveFailures++;
            if (consecutiveFailures >= FAILURE_THRESHOLD) {
                openUntil = Instant.now().plus(CIRCUIT_OPEN_DURATION);
                consecutiveFailures = 0;
                log.warn("🧯 Circuit OPEN dla providera na {}s", CIRCUIT_OPEN_DURATION.toSeconds());
            }
        }

        synchronized Instant openUntil() {
            return openUntil;
        }
    }
}