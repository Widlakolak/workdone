package com.workdone.backend.model;

import java.util.UUID;

public record OfferScoredEvent(
        UUID offerId,
        int score,
        String band
) {}