package com.workdone.backend.analysis;

public record OfferScoringResult(
        double score,
        boolean mustHaveSatisfied,
        String reasoning
) {
}