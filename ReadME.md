# 🚀 WorkDone – AI Agent Rekrutacyjny (Backend-Only)

WorkDone to inteligentny backendowy agent rekrutacyjny działający 24/7. System cyklicznie zbiera ogłoszenia o pracę, analizuje ich dopasowanie semantyczne do profilu kandydata (CV) przy użyciu modeli embeddingowych oraz LLM, a następnie przesyła wyselekcjonowane wyniki na Discord.

---

## ✅ Najnowsze Aktualizacje (Panel Kontrolny & Multi-Location) ✔️

System został rozbudowany o zaawansowane funkcje sterowania i precyzyjnego filtrowania:

1.  **Multi-Location Policy**: Możliwość definiowania reguł dla wielu miast naraz. Każde miasto może mieć własną politykę (np. Berlin: tylko Remote, Łódź: Hybryda/Stacjonarnie/Remote + limit dni w biurze).
2.  **Location Guard 2.0**: Inteligentny filtr wyciągający informacje o trybie pracy i liczbie dni w biurze bezpośrednio z treści ogłoszenia (Regex + Analiza kontekstowa).
3.  **Master Control Panel (Discord)**: Interaktywny panel sterowania na Discordzie pozwalający na:
    - Sprawdzanie aktualnego statusu filtrów.
    - Zmianę progów AI (`Semantic`, `Instant`, `Digest`) jednym kliknięciem.
    - Ręczne wyzwalanie procesu szukania ofert (`Run Ingestion`).
    - Odświeżanie skilli i seniority bezpośrednio z plików CV.
    - Zarządzanie "kolejką decyzji" (wyświetlanie ofert oczekujących na akceptację/odrzucenie).
4.  **Pending Queue System**: Oferty zakwalifikowane przez system otrzymują status `ANALYZED` i czekają na Twoją decyzję (przycisk "Aplikowano" / "Odrzuć").

---

## 🎮 Panel Sterowania na Discordzie

Aby wywołać interaktywny panel na serwerze Discord (jeśli nie jest jeszcze przypięty), wyślij zapytanie:
`POST https://workdone.qzz.io/api/admin/test/show-panel`

*Wskazówka: Po wywołaniu panelu na Discordzie, warto go **przypiąć (Pin)**, aby zawsze mieć pod ręką suwaki i przyciski statusu.*

---

## 🛠 Stack technologiczny

- **Backend:** Java 21, **Spring Boot 3.4+**
- **Architektura:** Modern Clean Code (Lombok, MapStruct, Spring AI)
- **Baza danych:** PostgreSQL 15 + **pgvector** (HNSW index)
- **AI Models:**
  - **Embedding:** Cohere Multilingual v3 (1024d) - Primary / OpenAI text-embedding-3-small - Fallback
  - **Scoring:** Groq Llama 3.3 70b (Primary) / OpenAI GPT-4o-mini / **Google Gemini 1.5 Flash**
- **Interakcje:** Discord Webhooks + Interactions API (Cloudflare Tunnel / Ngrok)

---

## 🧠 Logika Procesu (Flow)

```plain
CV → Parsing → AI Skill Extraction → Profile Vector (Cache)

Offer →
  Ingestion (Multiple Cities & Contexts) →
  LOCATION GUARD (Per-City Policy: Remote/Hybrid/Onsite + Days Limit) →
  TECH STACK EXTRACTION →
  VECTOR DEDUPLICATION (pgvector Search) →
  SEMANTIC MATCHING (Cosine Similarity) →
  AI MULTI-MODEL SCORING (Groq -> OpenAI -> Gemini) →
  STATUS ASSIGNMENT (ANALYZED) →
  Discord Notification (Instant / Daily Digest)
```

### Progi dopasowania (Zarządzane z Discorda/API)
- `Instant Alert` (⚡) -> Powiadomienie natychmiastowe.
- `Daily Digest` (📊) -> Zbiorczy raport raz na dobę.
- `Pending Decision` (⏳) -> Oferty czekające na Twoje kliknięcie "Aplikowano/Odrzuć".

---

## 🏗 Struktura Projektu

1.  **Ingestion** – Pobieranie danych: `Jooble`, `Jobicy`, `Remotive`, `RSS`. Obsługuje listę `SearchContext` dla każdej lokalizacji.
2.  **Analysis** – Silnik oceny: `LocationGuard`, `LocationPolicy`, `DynamicConfigService`.
3.  **Interaction** – `DiscordInteractionController` obsługujący kliknięcia przycisków na Discordzie.
4.  **Storage** – `PersistentOfferStore` (JPA) + `OfferVectorStore` (pgvector).

---

## 🗺 Roadmapa
- [x] **Multi-Location Support** (Zrealizowane)
- [x] **Discord Control Panel** (Zrealizowane)
- [ ] **Angular Admin Dashboard**: Pełna wizualizacja ofert i ustawień.
- [ ] **OCR Support**: Obsługa CV w formie obrazów.
- [ ] **Automatyczna Nauka**: Dynamiczne wagowanie na podstawie Twoich decyzji.
