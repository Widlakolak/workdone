-- Zmiana typu kolumny matching_score z INTEGER na DOUBLE PRECISION
ALTER TABLE job_offers
ALTER COLUMN matching_score TYPE DOUBLE PRECISION;

-- Dodanie brakującej kolumny priority_score
ALTER TABLE job_offers
ADD COLUMN priority_score DOUBLE PRECISION;