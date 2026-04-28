package com.workdone.backend.common.model;

import java.util.UUID;

public record CandidateProfileUpdatedEvent(
        UUID candidateId
) {}