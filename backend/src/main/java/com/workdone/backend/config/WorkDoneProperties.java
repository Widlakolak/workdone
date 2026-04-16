package com.workdone.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "workdone")
public record WorkDoneProperties(
        Profile profile,
        Matching matching,
        Scheduling scheduling,
        Discord discord,
        Providers providers
) {

    public record Profile(String inputDirectory) {
    }

    public record Matching(double instantThreshold,
                           double digestThreshold,
                           double archiveThreshold,
                           List<String> mustHaveKeywords) {
    }

    public record Scheduling(String ingestionCron, String digestCron, String zoneId) {
    }

    public record Discord(Webhook instant, Webhook digest) {
    }

    public record Webhook(boolean enabled, String url) {
    }

    public record Providers(
            ProviderConfig justjoinit,
            ProviderConfig nofluffjobs,
            ProviderConfig theprotocol,
            ProviderConfig upwork
    ) {
    }

    public record ProviderConfig(
            boolean enabled,
            String priority,
            Filters filters
    ) {
    }

    public record Filters(
            List<String> keywords,
            List<String> allowedCities,
            Boolean allowRemote,
            Boolean allowHybrid,
            String query,
            Integer minBudgetUsd
    ) {
    }
}