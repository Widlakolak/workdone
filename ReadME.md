# 🚀 WorkDone – AI Agent Rekrutacyjny (Backend-Only)

WorkDone to inteligentny backendowy agent rekrutacyjny działający 24/7. System cyklicznie zbiera ogłoszenia o pracę, analizuje ich dopasowanie semantyczne do profilu kandydata (CV) przy użyciu modeli embeddingowych oraz LLM, a następnie przesyła wyselekcjonowane wyniki na Discord.

---

## ✅ Aktualny Status: AI-Native Refactor (ZREALIZOWANY ✔️)

[![Build & Push WorkDone](https://github.com/Widlakolak/workdone/actions/workflows/deploy.yml/badge.svg)](https://github.com/Widlakolak/workdone/actions/workflows/deploy.yml)

System realizuje zaawansowany pipeline AI zoptymalizowany pod kątem monitoringu i niezawodności:
1.  **CV Embedding & Skill Extraction**: Profil kandydata jest wektoryzowany (Cohere) i analizowany pod kątem kluczowych technologii (LLM).
2.  **Smart Ingestion**: Pobieranie ofert z wielu źródeł (**Jooble API**, **Jobicy API**, **Remotive API**, **RSS Aggregator**) z filtrowaniem lokalizacji (**LocationGuard**).
3.  **Single Embedding Pattern**: Każda unikalna oferta jest wektoryzowana tylko raz dla celów deduplikacji i dopasowania.
4.  **Vector Deduplication (pgvector)**: Sprawdzanie duplikacji w PostgreSQL **PRZED** kosztowną analizą LLM.
5.  **Multi-Level AI Scoring & Fallback**: Głęboka analiza LLM z kaskadowym systemem odporności na awarie (**Groq -> OpenAI -> Gemini -> Semantic Fallback**) i alertami na Discordzie.
6.  **Full Monitoring Console**: Powiadomienia Discord o każdym etapie (Start/Finish Ingestion), błędach providerów i wyczerpaniu limitów AI.
7.  **Dynamic Control Panel**: Zarządzanie progami i konfiguracją prosto z Discorda.

---

## 🛠 Stack technologiczny

- **Backend:** Java 21, **Spring Boot 4.0.5**
- **Architektura:** Modern Clean Code (Lombok, MapStruct, **Spring AI 2.0.0-M4**)
- **Baza danych:** PostgreSQL 15 + **pgvector** (HNSW index)
- **AI Models:**
  - **Embedding:** Cohere Multilingual v3 (1024d) - Primary / OpenAI text-embedding-3-small - Fallback
  - **Scoring:** Groq Llama 3.3 70b (Primary) / OpenAI GPT-4o-mini / **Google Gemini 2.5 Flash Lite**
- **Operacje:** Docker + GitHub Actions (CI/CD)
- **Kanał użytkownika:** Discord (Instant alerts + Daily Digest + Interaction Buttons + System Status Alerts)

---

## 🧠 Logika Procesu (Flow)

```plain
CV → Parsing (Apache Tika) → AI Skill Extraction → Embedding (Cohere) → Profile Vector (Cache)

Offer →
  Ingestion (Jooble / Jobicy / Remotive / RSS) →
  LOCATION GUARD (Dynamic Filter) →
  TECH STACK EXTRACTION →
  SINGLE EMBEDDING (Cohere/OpenAI Fallback) →
  VECTOR DEDUPLICATION (pgvector Search) →
  IF Duplicate THEN: Skip
  IF Unique THEN:
    SAVE TO VECTOR STORE →
    SEMANTIC MATCHING (Cosine Similarity) →
    IF Score > dynamic_threshold THEN:
      AI MULTI-MODEL SCORING (Groq -> OpenAI -> Gemini) →
      (Alert Discord on Model Fallback/Limits)
    PRIORITY BOOSTING →
    Discord Notification (Instant/Digest/Alerts)
```

### Progi dopasowania (Dynamiczne)
- `score >= 90` → Discord **Instant Alert**
- `60 <= score < 90` → Discord **Daily Digest**
- `40 <= score < 60` → Baza danych (Tracking)
- `< 40` → Status `ARCHIVED`

---

## 🏗 Struktura Projektu

1.  **Ingestion** – Pobieranie danych: `RemotiveJobProvider` (API), `JobicyJobProvider` (API), `JoobleJobProvider` (API), `RssJobProvider` (RSS).
2.  **Analysis** – Silnik oceny: `LocationGuard`, `OfferScoringService` (Multi-model), `DynamicConfigService`.
3.  **Notification** – `DiscordNotifier` z obsługą alertów systemowych i interaktywnych paneli.
4.  **Storage** – `PersistentOfferStore` (JPA) + `OfferVectorStore` (pgvector).

---

## 📁 Źródła Danych (Zasoby)

### Obecne (API):
- **Jooble API**
- **Jobicy API**
- **Remotive API** (Zmigrowane z RSS)

### Obecne (RSS):
- **RemoteOK**
- **WeWorkRemotely**

---

## ▶️ Konfiguracja

### Zmienne Środowiskowe:
- `COHERE_API_KEY`: Embedding 1024d.
- `GROQ_API_KEY`: Primary scoring.
- `OPENAI_API_KEY`: Fallback scoring/embedding.
- `GEMINI_API_KEY`: Tertiary scoring.
- `DISCORD_PUBLIC_KEY`: Weryfikacja interakcji.
- `DB_HOST`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`: PostgreSQL + pgvector.

---

## 🗺 Roadmapa
- [ ] **Automatyczna Nauka**: Dynamiczne wagowanie na podstawie decyzji Applied/Reject.
- [ ] **OCR Support**: Obsługa CV w formie obrazów.
- [ ] **Growth Opportunity**: Wykrywanie ofert "prawie pasujących".
