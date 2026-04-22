package com.workdone.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateProfileSnapshot {
    private UUID id;
    private String fullText;
    private String skills;
    private String experience;
    private String embeddingId;
}
