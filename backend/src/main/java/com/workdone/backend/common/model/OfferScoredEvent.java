package com.workdone.backend.common.model;

import java.util.UUID;

public record OfferScoredEvent(
        UUID offerId,
        int score,
        String band
) {}