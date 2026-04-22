ALTER TABLE job_offers ADD COLUMN published_at TIMESTAMP WITH TIME ZONE;
CREATE INDEX idx_offers_published_at ON job_offers(published_at);
