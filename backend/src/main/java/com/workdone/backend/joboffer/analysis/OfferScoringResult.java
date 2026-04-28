package com.workdone.backend.joboffer.analysis;

public record OfferScoringResult(
        double score,
        boolean mustHaveSatisfied,
        String reasoning
) {
}