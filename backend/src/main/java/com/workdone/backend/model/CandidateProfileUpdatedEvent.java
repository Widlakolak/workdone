package com.workdone.backend.model;

import java.util.UUID;

public record CandidateProfileUpdatedEvent(
        UUID candidateId
) {}