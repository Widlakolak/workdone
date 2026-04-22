package com.workdone.backend.ingestion.remotive;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "workdone.providers.remotive")
public record RemotiveProperties(
        String baseUrl,
        boolean enabled
) {}
