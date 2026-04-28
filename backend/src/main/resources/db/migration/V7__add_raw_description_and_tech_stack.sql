ALTER TABLE job_offers
ADD COLUMN IF NOT EXISTS raw_description TEXT;

ALTER TABLE job_offers
ADD COLUMN IF NOT EXISTS tech_stack JSONB DEFAULT '[]'::jsonb;

UPDATE job_offers
SET raw_description = content
WHERE raw_description IS NULL
  AND content IS NOT NULL;