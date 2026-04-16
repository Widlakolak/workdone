package com.workdone.backend.ingestion.justjoinit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "workdone.providers.justjoinit")
public record JustJoinItProperties(
        boolean enabled,
        String baseUrl,
        String offersPath,
        String priority,
        Filters filters
) {
    public record Filters(
            List<String> keywords,
            List<String> allowedCities,
            boolean allowRemote,
            boolean allowHybrid
    ) {
    }
}