package com.workdone.backend.ingestion.jobicy;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "workdone.providers.jobicy")
public record JobicyProperties(
        String baseUrl,
        boolean enabled
) {}
