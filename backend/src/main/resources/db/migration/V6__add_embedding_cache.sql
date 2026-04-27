CREATE TABLE embedding_cache (
    text_hash VARCHAR(64) PRIMARY KEY, -- SHA-256 z tekstu
    vector vector(1024) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_embedding_cache_hash ON embedding_cache(text_hash);