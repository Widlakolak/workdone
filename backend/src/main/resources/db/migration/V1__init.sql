-- 1. Włączamy rozszerzenia
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Tabela łącząca dane domenowe z silnikiem wektorowym AI
CREATE TABLE job_offers (
    -- Wymagane przez Spring AI (UUID jest wydajniejsze dla wektorów)
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Pola biznesowe
    source_url TEXT NOT NULL UNIQUE,
    fingerprint VARCHAR(255) NOT NULL UNIQUE,
    title VARCHAR(255),
    company VARCHAR(255),
    location VARCHAR(255),
    matching_score INT DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',

    -- Wymagane przez Spring AI: treść oferty (do analizy) i metadane (JSON)
    content TEXT,
    metadata JSONB DEFAULT '{}'::jsonb,

    -- Wymagane przez Spring AI: Wektor (768 wymiarów dla modelu Gemini)
    embedding vector(768),

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Indeksy dla szybkości działania na QNAP
CREATE INDEX idx_offers_status ON job_offers(status);
CREATE INDEX idx_offers_fingerprint ON job_offers(fingerprint);
-- Indeks HNSW dla szybkich wyszukiwań semantycznych (kluczowe przy słabym CPU)
CREATE INDEX idx_offers_embedding ON job_offers USING hnsw (embedding vector_cosine_ops);