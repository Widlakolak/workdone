package com.workdone.backend.model;

import java.util.UUID;

public record CandidateProfileSnapshot(
        UUID id,
        String fullText,
        String skills,
        String experience,
        String embeddingId
) {}