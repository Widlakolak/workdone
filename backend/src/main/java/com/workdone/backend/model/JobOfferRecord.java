package com.workdone.backend.model;

import java.time.LocalDateTime;
import java.util.List;

public record JobOfferRecord(
        String id,
        String fingerprint,
        String title,
        String companyName,
        String sourceUrl,
        String location,
        String rawDescription,
        String salaryRange,
        List<String> techStack,
        Double matchingScore,
        OfferStatus status,
        LocalDateTime publishedAt,
        String sourcePlatform
) {
    public JobOfferRecord {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException("Oferta musi posiadać unikalny adres URL (sourceUrl)");
        }
    }

    public JobOfferRecord withAnalysis(Double newScore, OfferStatus newStatus) {
        return new JobOfferRecord(
                id,
                fingerprint,
                title,
                companyName,
                sourceUrl,
                location,
                rawDescription,
                salaryRange,
                techStack,
                newScore,
                newStatus,
                publishedAt,
                sourcePlatform
        );
    }

    public JobOfferRecord withMatchingScore(Double score) {
        return new JobOfferRecord(
                this.id(),
                this.fingerprint(),
                this.title(),
                this.companyName(),
                this.sourceUrl(),
                this.location(),
                this.rawDescription(),
                this.salaryRange(),
                this.techStack(),
                score,
                this.status(),
                this.publishedAt(),
                this.sourcePlatform()
        );
    }
}