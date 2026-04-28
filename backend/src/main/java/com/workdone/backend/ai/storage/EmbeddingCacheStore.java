package com.workdone.backend.ai.storage;

import java.util.Optional;

public interface EmbeddingCacheStore {
    Optional<float[]> get(String textHash);
    void put(String textHash, float[] vector);
}