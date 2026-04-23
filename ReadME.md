# 🚀 WorkDone – AI Agent Rekrutacyjny (Backend)

WorkDone to backendowy agent rekrutacyjny działający cyklicznie. Zbiera oferty pracy z wielu źródeł, filtruje je regułami i scoringiem semantycznym, a następnie wysyła powiadomienia na Discord.

---

## ✅ Co jest najważniejsze teraz

1. **Multi-Location Policy** – możesz utrzymywać wiele polityk lokalizacji naraz (remote/hybrid/onsite + max dni w biurze).
2. **Dynamiczny panel Discord** – status i konfiguracja „w locie” bez restartu aplikacji.
3. **Fallback „najlepsza oferta po skanie”** – opcjonalny tryb: jeśli po skanie nie było żadnej oferty INSTANT, system może wysłać jedną najlepszą ofertę.
4. **Pending Queue** – oferty `ANALYZED` czekają na decyzję (`Aplikowano` / `Odrzuć`).

---

## 🎮 Sterowanie z Discorda

### Panel
Aby wysłać panel sterowania:

```http
POST /api/admin/test/show-panel
```

### Najważniejsze akcje
- `config|status`
- `config|refresh_cv`
- `config|use_cv_skills`
- `config|run_ingestion`
- `config|best_offer_fallback|true` (włącz fallback najlepszej oferty)
- `config|best_offer_fallback|false` (wyłącz fallback)

---

## ⚙️ Jak działa pipeline ofert

```plain
CV files -> merge -> embedding + parsing (keywords/seniority/location)

Scheduler/manual trigger ->
  Ingestion (providers x search contexts) ->
  Enrich + dedup ->
  Must-Have + scoring semantyczny ->
  (opcjonalnie) Deep AI scoring ->
  Klasyfikacja (INSTANT / DIGEST / ARCHIVE) ->
  Zapis oferty ->
  Discord notify:
    - zawsze: INSTANT (bez zmian)
    - opcjonalnie: best-offer fallback, jeśli w skanie nie było INSTANT
```

> Uwaga: fallback „najlepszej oferty” **nie zastępuje** INSTANTów. INSTANTy nadal idą normalnie.

---

## ⏱ Harmonogram

Domyślnie:
- ingestion: co 2 godziny
- digest: raz dziennie

Konfiguracja w `application.yaml` (`workdone.scheduling.*`).

---

## 🛠 Stack technologiczny

- **Backend:** Java 21, Spring Boot 4.x
- **AI/ML:** Spring AI + OpenAI/Gemini/Groq, embeddingi: **Cohere (primary) → OpenAI (fallback)**
- **Storage:** PostgreSQL + pgvector
- **Integracje:** Discord Webhooks + Discord Interactions API

---

## 📂 Struktura

1. **ingestion/** – dostawcy ofert i budowa `SearchContext`
2. **analysis/** – filtrowanie, scoring, klasyfikacja, config runtime
3. **orchestration/** – pipeline end-to-end i harmonogram
4. **interaction/** – endpointy API i obsługa kliknięć Discord
5. **storage/** – zapis ofert + wektory

---

## 🗺 Roadmapa
- [x] Multi-location
- [x] Discord control panel
- [x] Fallback najlepszej oferty po skanie (toggle)
- [ ] Dashboard webowy
- [ ] OCR dla CV obrazkowych
- [ ] Uczenie preferencji na podstawie decyzji użytkownika