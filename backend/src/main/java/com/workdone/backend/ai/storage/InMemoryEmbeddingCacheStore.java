package com.workdone.backend.ai.storage;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Makietowa (InMemory) wersja cache'u embeddingów dla testów.
 */
@Component
@Profile("test")
public class InMemoryEmbeddingCacheStore implements EmbeddingCacheStore {

    private final Map<String, float[]> cache = new ConcurrentHashMap<>();

    @Override
    public Optional<float[]> get(String textHash) {
        return Optional.ofNullable(cache.get(textHash));
    }

    @Override
    public void put(String textHash, float[] vector) {
        cache.put(textHash, vector);
    }
}