package com.workdone.backend.common.util;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderCallExecutorTest {

    private final ProviderErrorMetrics metrics = Mockito.mock(ProviderErrorMetrics.class);
    private final ProviderCallExecutor executor = new ProviderCallExecutor(metrics);

    @Test
    void shouldRecord4xxMetric() {
        assertThatThrownBy(() -> executor.execute("JOOBLE", () -> {
            throw HttpClientErrorException.create(
                    HttpStatus.BAD_REQUEST, "bad request", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8
            );
        })).isInstanceOf(HttpClientErrorException.class);

        Mockito.verify(metrics).increment("JOOBLE", ProviderErrorType.HTTP_4XX);
    }

    @Test
    void shouldRecord5xxMetric() {
        assertThatThrownBy(() -> executor.execute("JOBICY", () -> {
            throw HttpServerErrorException.create(
                    HttpStatus.INTERNAL_SERVER_ERROR, "boom", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8
            );
        })).isInstanceOf(HttpServerErrorException.class);

        Mockito.verify(metrics).increment("JOBICY", ProviderErrorType.HTTP_5XX);
    }

    @Test
    void shouldOpenCircuitAfterConsecutiveFailures() {
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> executor.execute("REMOTIVE", () -> {
                throw new ResourceAccessException("Read timed out");
            })).isInstanceOf(ResourceAccessException.class);
        }

        assertThatThrownBy(() -> executor.execute("REMOTIVE", () -> "ok"))
                .isInstanceOf(ProviderCircuitBreakerOpenException.class);

        Mockito.verify(metrics).increment("REMOTIVE", ProviderErrorType.CIRCUIT_OPEN);
    }
}