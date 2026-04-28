package com.workdone.backend.ai.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Profile("!test")
public class JdbcAiAnalysisCacheStore implements AiAnalysisCacheStore {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void put(String fingerprint, double score, String reasoning, String modelName) {
        String sql = """
            INSERT INTO ai_analysis_cache (fingerprint, ai_score, reasoning, model_name)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (fingerprint) DO UPDATE SET
                ai_score = EXCLUDED.ai_score,
                reasoning = EXCLUDED.reasoning,
                model_name = EXCLUDED.model_name
            """;
        jdbcTemplate.update(sql, fingerprint, score, reasoning, modelName);
    }

    @Override
    public Optional<AiScoreEntry> get(String fingerprint) {
        String sql = "SELECT ai_score, reasoning FROM ai_analysis_cache WHERE fingerprint = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
                        new AiScoreEntry(rs.getDouble("ai_score"), rs.getString("reasoning")),
                fingerprint
        ).stream().findFirst();
    }
}