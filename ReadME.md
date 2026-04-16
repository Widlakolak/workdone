# 🚀 WorkDone – AI Agent Rekrutacyjny (Backend-Only MVP)

WorkDone to backendowe narzędzie, które działa 24/7 i pomaga znaleźć **realne oferty pracy**.
System cyklicznie zbiera ogłoszenia, analizuje dopasowanie do profilu kandydata i wysyła wyniki na Discord.

---

## ✅ Aktualny cel (bez Sprintu 5 / bez frontendu)

MVP działa bez UI i jest obsługiwane przez:
- harmonogram (`@Scheduled`),
- pliki profilu wrzucane do katalogu wejściowego,
- Discord webhooki (instant + daily digest),
- statusy ofert w backendzie.

---

## 🛠 Stack technologiczny

- **Backend:** Java 21, Spring Boot 4.0.5
- **Architektura:** Modular Monolith / Event-Driven ready
- **Messaging (kolejne etapy):** Apache Kafka (KRaft)
- **Baza danych:** PostgreSQL + pgvector (docelowo)
- **AI:** Gemini API (kolejne etapy), obecnie scoring MVP oparty o profil + `must-have`
- **Operacje:** Docker + QNAP NAS + Cloudflare Tunnel
- **Kanał użytkownika:** Discord (instant alerts + digest + interakcje)

---

## 🧠 Logika MVP (ustalona)

### Harmonogram
- Ingestion: **co 2 godziny (24/7)**
- Daily Digest: **18:00 Europe/Warsaw**

### Źródła (MVP)
- JustJoinIT (aktywne)
- Upwork (szkielet)
- NoFluffJobs (szkielet)
- theprotocol.it (szkielet)

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
- Wrzucasz tam pliki CV / screenshoty LinkedIn / certyfikaty
- Pliki tekstowe (`txt`, `md`, `json`) są czytane bezpośrednio
- OCR dla obrazów/PDF jest oznaczony jako kolejny krok (`OCR_PENDING`)

---

## 🏗 Moduły backendu (obecnie)

1. **Ingestion** – providerzy ofert (`JobProvider` + fetchery)
2. **Analysis** – scoring, klasyfikacja progów, fingerprinty
3. **Orchestration** – job scheduler i pipeline end-to-end
4. **Storage** – store MVP + deduplikacja
5. **Notification** – Discord instant/digest
6. **Interaction** – `DiscordInteractionController` (`Applied` / `Reject` → update statusu)
7. **Profile** – budowanie kontekstu kandydata z katalogu wejściowego

---

## ▶️ Konfiguracja `application.yaml` (najważniejsze)

- `workdone.profile.input-directory`
- `workdone.matching.*`
- `workdone.scheduling.*`
- `workdone.discord.instant.*`
- `workdone.discord.digest.*`
- `workdone.providers.*`

---

## 🗺 Roadmapa po MVP

- Kafka producer/consumer + DLQ
- Integracja Gemini do oceny i ekstrakcji `must-have` z dokumentów
- OCR pipeline dla PDF/screenshotów
- Persistencja relacyjna + pgvector
- Automatyczne wykrywanie `GHOST_JOB`
- (Opcjonalnie później) frontend dashboard