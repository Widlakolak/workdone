package com.workdone.backend.ingestion.jooble;

public record JoobleRequest(
        String keywords,
        String location
) {}