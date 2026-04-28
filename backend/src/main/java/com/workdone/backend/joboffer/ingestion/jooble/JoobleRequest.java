package com.workdone.backend.joboffer.ingestion.jooble;

public record JoobleRequest(
        String keywords,
        String location
) {}