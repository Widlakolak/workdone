-- D:/dev/workdone/backend/src/main/resources/db/migration/V5__add_ai_analysis_cache.sql

CREATE TABLE ai_analysis_cache (
    fingerprint VARCHAR(255) PRIMARY KEY,
    ai_score DOUBLE PRECISION NOT NULL,
    reasoning TEXT,
    model_name VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_cache_fingerprint ON ai_analysis_cache(fingerprint);