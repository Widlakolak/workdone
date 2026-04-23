package com.workdone.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "workdone")
public record WorkDoneProperties(
        Profile profile,
        Matching matching,
        Scheduling scheduling,
        Discord discord,
        Providers providers,
        Search search 
) {
    public record Profile(String inputDirectory) {}

    public record Matching(
            double instantThreshold,
            double digestThreshold,
            double archiveThreshold,
            double semanticThreshold,
            List<String> mustHaveKeywords,
            double aiThresholdMin,
            double aiThresholdMax
    ) {}

    public record Scheduling(String ingestionCron, String digestCron, String zoneId) {}

    // Dodajemy pole token do konfiguracji Discorda
    public record Discord(String token, Webhook instant, Webhook digest) {}

    public record Webhook(boolean enabled, String url) {}

    public record Providers(ProviderConfig jooble) {}

    public record ProviderConfig(
            boolean enabled,
            String url,
            String priority,
            Filters filters
    ) {}

    public record Filters(
            List<String> keywords,
            List<String> allowedCities,
            Boolean allowRemote,
            Boolean allowHybrid,
            String query,
            Integer minBudgetUsd
    ) {}

    public record Search(
            String defaultLocation,
            boolean allowRemote
    ) {}
}
