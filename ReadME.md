# 🚀 WorkDone – AI Agent Rekrutacyjny (Backend-Only MVP)

WorkDone to backendowe narzędzie, które działa 24/7 i pomaga znaleźć **realne oferty pracy**.  
System cyklicznie zbiera ogłoszenia, analizuje dopasowanie do profilu kandydata i wysyła wyniki na Discord.

---

## ✅ Aktualny cel (bez Sprintu 5 / bez frontendu)

[![Build & Push WorkDone](https://github.com/Widlakolak/workdone/actions/workflows/deploy.yml/badge.svg)](https://github.com/Widlakolak/workdone/actions/workflows/deploy.yml)

MVP działa bez UI i jest obsługiwane przez:
- harmonogram (`@Scheduled`),
- pliki profilu wrzucane do katalogu wejściowego,
- Discord webhooki (instant + daily digest),
- statusy ofert w backendzie.

---

## ✅ Status projektu

### ETAP 0 – ZREALIZOWANY ✔️

System MVP działa end-to-end bez UI i obejmuje:

- harmonogram (@Scheduled) – ingestion + digest
- ingestion ofert z wielu źródeł
- scoring (keyword + heurystyki)
- priority scoring (stage 0)
- deduplikacja (URL + fingerprint SHA-256)
- profil kandydata z katalogu /data/workdone/profile-input
- Discord (instant + daily digest)
- interakcje Discord (Applied / Reject → update statusu)
- in-memory store

---

## 🛠 Stack technologiczny

- **Backend:** Java 21, Spring Boot 4.0.5
- **Architektura:** Modular Monolith / Event-Driven ready
- **Messaging (kolejne etapy):** Apache Kafka (KRaft)
- **Baza danych:** PostgreSQL + pgvector (docelowo)
- **AI:** Spring AI 2.0.0-M4
  * ChatClient – scoring / analiza
  * EmbeddingModel – embedding CV + ofert (Etap 2)  
- **Operacje:** Docker + QNAP NAS + Cloudflare Tunnel
- **Kanał użytkownika:** Discord (instant alerts + digest + interakcje)

---

## 🧠 Logika MVP (ustalona)

### Harmonogram
- Ingestion: **co 2 godziny (24/7)**
- Daily Digest: **18:00 Europe/Warsaw**

### Źródła (MVP)
- Jooble
  - zaplanowane API - Adzuna API, Arbeitnow API, USAJOBS API, Greenhouse job boards, Lever jobs API
  - zaplanowane RSS - Jobicy RSS, Remote OK RSS, We Work Remotely RSS, Remotive RSS

### Progi dopasowania
- `score >= 90` → Discord **Instant**
- `60 <= score < 90` → Discord **Daily Digest**
- `40 <= score < 60` → zapis tylko do DB/store
- `< 40` → status `ARCHIVED`

### Reguły jakości
- Twarde `must-have` (na MVP z configu, docelowo AI z dokumentów)
- Filtrowanie lokalizacji: Łódź / hybryda PL / remote
- Deduplikacja hybrydowa:
  1. `sourceUrl` (unikalność techniczna)
  2. `fingerprint = SHA-256(normalized(title + company + city))`

---

## 📁 Profil kandydata (MVP)

- Katalog wejściowy: **`/data/workdone/profile-input`**
- CV / LinkedIn / certyfikaty jako input kontekstowy
- Pliki tekstowe (`txt`, `md`, `json`) są czytane bezpośrednio
- OCR dla PDF/obrazów: OCR_PENDING (kolejny etap)

---

## 🏗 Moduły backendu (obecnie)

1. **Ingestion** – lejek ofert (`JobProvider` + fetchery)
2. **Analysis**
	* OfferClassificationService
	* OfferMatchingService
	* OfferFingerprintFactory
	* OfferScoringService
	* OfferEmbeddingService (przygotowane pod Etap 2)
3. **Orchestration** – job scheduler i pipeline end-to-end
4. **Storage** – store MVP + deduplikacja
5. **Notification** – Discord instant/digest
6. **Interaction** – `DiscordInteractionController` (`Applied` / `Reject` → update statusu)
7. **Profile** – budowanie kontekstu kandydata z katalogu wejściowego  
8. **Spring AI Integration** – `OfferScoringService` (ChatClient) oraz `OfferEmbeddingService` (EmbeddingModel) gotowe do podpięcia w pipeline ingestion/analysis

## 🤖 AI (obecny stan)
- Etap 0: scoring heurystyczny + must-have config
- przygotowana integracja Spring AI:
  - ChatClient (scoring logic – przyszłe rozszerzenie)
  - EmbeddingModel (CV + offers embeddings)

---

## ▶️ Konfiguracja `application.yaml`

- `workdone.profile.input-directory`
- `workdone.matching.*`
- `workdone.scheduling.*`
- `workdone.discord.instant.*`
- `workdone.discord.digest.*`
- `workdone.providers.*`

---

## 🗺 Roadmapa po MVP
### 🔥 Etap 0 - zakończony
**Cel: system działa i już daje przewagę**
### 🟡 Etap 1 – Smart Filtering
- lepszy junior detection
- location intelligence
- tuning progów scoringu
### 🟠 Etap 2 – Embedding Matching 
- CV embedding (pgvector ready)
- offer embedding storage
- semantic similarity scoring
- hybrid scoring (keyword + embedding)
### 🔴 Etap 3 – Learning System
- feedback loop (Applied / Rejected)
- dynamic weighting
- similarity learning
### 🔵 Etap 4 – Discord Control Panel
- sterowanie filtrami przez Discord
- runtime preferences
### 🟣 Etap 5 – Opportunity Discovery
- low-match high-potential detection
- "Growth Opportunity" pipeline