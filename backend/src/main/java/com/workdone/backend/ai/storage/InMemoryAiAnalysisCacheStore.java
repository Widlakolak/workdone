package com.workdone.backend.ai.storage;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("test")
public class InMemoryAiAnalysisCacheStore implements AiAnalysisCacheStore {
    private final Map<String, AiScoreEntry> cache = new ConcurrentHashMap<>();

    @Override
    public void put(String fingerprint, double score, String reasoning, String modelName) {
        cache.put(fingerprint, new AiScoreEntry(score, reasoning));
    }

    @Override
    public Optional<AiScoreEntry> get(String fingerprint) {
        return Optional.ofNullable(cache.get(fingerprint));
    }
}