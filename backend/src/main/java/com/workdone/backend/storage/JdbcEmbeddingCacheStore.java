package com.workdone.backend.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class JdbcEmbeddingCacheStore {

    private final JdbcTemplate jdbcTemplate;

    public Optional<float[]> get(String textHash) {
        String sql = "SELECT vector FROM embedding_cache WHERE text_hash = ?";
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                String vectorStr = rs.getString("vector");
                return parseVector(vectorStr);
            }, textHash).stream().findFirst();
        } catch (Exception e) {
            log.warn("⚠️ Błąd odczytu z cache embeddingów dla hash: {}", textHash);
            return Optional.empty();
        }
    }

    public void put(String textHash, float[] vector) {
        String sql = "INSERT INTO embedding_cache (text_hash, vector) VALUES (?, ?::vector) ON CONFLICT DO NOTHING";
        try {
            jdbcTemplate.update(sql, textHash, formatVector(vector));
        } catch (Exception e) {
            log.error("❌ Błąd zapisu do cache embeddingów: {}", e.getMessage());
        }
    }

    private String formatVector(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private float[] parseVector(String vectorStr) {
        if (vectorStr == null || vectorStr.isBlank()) return new float[0];
        String cleaned = vectorStr.replace("[", "").replace("]", "");
        String[] parts = cleaned.split(",");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i]);
        }
        return vector;
    }
}