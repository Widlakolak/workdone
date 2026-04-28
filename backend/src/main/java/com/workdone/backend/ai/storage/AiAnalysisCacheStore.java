package com.workdone.backend.ai.storage;

import java.util.Optional;

public interface AiAnalysisCacheStore {
    void put(String fingerprint, double score, String reasoning, String modelName);
    Optional<AiScoreEntry> get(String fingerprint);

    record AiScoreEntry(double score, String reasoning) {}
}