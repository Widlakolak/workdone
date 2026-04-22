-- Musimy usunąć indeks przed zmianą typu kolumny
DROP INDEX IF EXISTS idx_offers_embedding;

-- Zmiana wymiarów z 768 na 1024
ALTER TABLE job_offers ALTER COLUMN embedding TYPE vector(1024);

-- Przywrócenie indeksu HNSW
CREATE INDEX idx_offers_embedding ON job_offers USING hnsw (embedding vector_cosine_ops);
