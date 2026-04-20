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
        Double priorityScore,
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
                priorityScore,
                newStatus,
                publishedAt,
                sourcePlatform
        );
    }

    public JobOfferRecord withMatchingScore(Double newScore) {
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
                newScore,
                this.priorityScore,
                this.status(),
                this.publishedAt(),
                this.sourcePlatform()
        );
    }

    public JobOfferRecord withPriorityScore(Double newPriorityScore) {
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
                this.matchingScore(),
                newPriorityScore,
                this.status(),
                this.publishedAt(),
                this.sourcePlatform()
        );
    }

    public JobOfferRecord withStatus(OfferStatus newStatus) {
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
                this.matchingScore(),
                this.priorityScore(),
                newStatus,
                this.publishedAt(),
                this.sourcePlatform()
        );
    }

    public JobOfferRecord withIdAndFingerprint(String newId, String newFingerprint) {
        return new JobOfferRecord(
                newId,
                newFingerprint,
                this.title(),
                this.companyName(),
                this.sourceUrl(),
                this.location(),
                this.rawDescription(),
                this.salaryRange(),
                this.techStack(),
                this.matchingScore(),
                this.priorityScore(),
                this.status(),
                this.publishedAt(),
                this.sourcePlatform()
        );
    }
}