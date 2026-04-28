package com.workdone.backend.joboffer.ingestion.jooble;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "workdone.providers.jooble")
public record JoobleProperties(
        boolean enabled,
        String apiKey,
        String url,
        String baseUrl,
        Filters filters
) {

    public String resolvedUrl() {
        if (url != null && !url.isBlank()) {
            return url;
        }
        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl;
        }
        return null;
    }

    public record Filters(
            List<String> keywords,
            String location,
            boolean allowRemote
//            Integer salaryMin,
//            Integer radius
    ) {}
}
