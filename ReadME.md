# 🚀 WorkDone – Twój osobisty agent rekrutacyjny
System klasy korporacyjnej służący do inteligentnego gromadzenia, filtrowania i analizowania ofert pracy  
z wykorzystaniem AI (Gemini).  
Zoptymalizowany pod kątem maksymalnej efektywności na ograniczonych zasobach (QNAP NAS).

---

## 🛠 Stos technologiczny
* Backend: Java 21, Spring Boot 4.0.5
* Architektura: Modular Monolith / Event-Driven
* Komunikacja: Apache Kafka (tryb KRaft - RAM efficiency)
* Baza danych: PostgreSQL + pgvector (wyszukiwanie semantyczne)
* AI: Google Gemini API (RestClient)
* Frontend: Angular (panel zarządzania) + Nginx
* DevOps: Docker (Memory Limits), Cloudflare Tunnel (dostęp z zewnątrz dla Discord Webhooks)

---

## 🏗 Architektura systemu
### System opiera się na „lejku danych”:
1. **Ingestion:** Fetchery (API/RSS) agregują oferty (JustJoinIT, Upwork, NoFluffJobs i inne).
2. **Messaging:** Kafka buforuje dane, zapewniając stabilność na słabszym procesorze NAS.
3. **Intelligence:** Gemini AI porównuje ofertę względem profilu kandydata.
4. **Storage:** Zapis do bazy wektorowej (pgvector).
5. **Delivery:** Natychmiastowe alerty o „perełkach” + wieczorny raport zbiorczy na Discordzie.
6. **Interaction:** Zarządzanie statusem oferty bezpośrednio z poziomu powiadomienia (przyciski Discord).

---

## 🗺 Plan projektu

### Sprint 0: Fundamenty Infrastruktury
* Inicjalizacja monorepo (Gradle Kotlin DSL).  
* Definicja podstawowego modelu danych (JobOfferRecord).  
* Konfiguracja `docker-compose.yml` (Postgres + pgvector, Kafka KRaft).  
* **Cloudflare Tunnel:** Konfiguracja bezpiecznego kanału dla interakcji z Discordem.

### Sprint 1: Data Ingestion (Silnik Agregacji)
* Implementacja `JobProvider` (Wzorzec Strategy).  
* **Multi-source Fetchers:** Agregacja ofert z JustJoinIT (API), Upwork (RSS), NoFluffJobs i innych źródeł.  
* **Filtrowanie geograficzne:** Logika akceptująca tylko: Łódź, hybryda (PL), Remote (Global).  
* Mechanizm deduplikacji (hashing URL/tytułów) – jedna oferta pojawia się w systemie tylko raz.

### Sprint 2: Event-Driven Processing (Kafka)
* Konfiguracja producenta Kafka (wysyłanie ofert na topic).  
* Konfiguracja konsumenta Kafka (odbiornik do analizy).  
* **Implementacja Dead Letter Queue (DLQ):** Obsługa błędów przetwarzania AI.

### Sprint 3: AI Engine & Rozszerzony Profil (Mózg)
* **Budowa "Cyfrowego Profilu Kandydata":** JSON na bazie CV, LinkedIn (screeny/pdf) i certyfikatów.  
* Integracja Gemini API: Prompt Engineering do oceny dopasowania (Match Score).  
* Implementacja `pgvector` do wyszukiwania semantycznego podobnych ofert.

### Sprint 4: Operacje Discord (MVP "w akcji")
* **Real-time Alerty:** Natychmiastowe powiadomienia o ofertach `>90% match`.
* **Interactive Buttons:** Przyciski "Aplikowałem" / "Odrzuć" pod wiadomością na Discordzie (aktualizacja bazy).
* **Daily Digest:** Wieczorne podsumowanie ofert z całego dnia (`@Scheduled`).

### Sprint 5: Frontend
* Budowa Dashboardu w Angularze (statystyki, historia aplikacji).
* Zabezpieczenie API (Spring Security).
* Wystawienie panelu przez Nginx i Cloudflare Tunnel.

---

## 🚀 Przyszłe ulepszenia
* **Moduł Selenium:** Scrapery dla portali bez API (np. LinkedIn, Pracuj.pl).  
* **AI Cover Letter:** Automatyczne generowanie draftu listu motywacyjnego pod konkretną ofertę.  
* **Multi-tenancy & SSO:** Dodanie obsługi wielu użytkowników poprzez logowanie Google (OAuth2), pozwalające na tworzenie oddzielnych profili i historii przeglądania.  
* **Automatyzacja Aplikacji:** Funkcja (Opt-In) pozwalająca systemowi AI na samodzielne wysyłanie wygenerowanych listów motywacyjnych na oferty ze 100% dopasowaniem.  
* **Analityka Rynku (Market Insights):** Generowanie wykresów trendów w technologiach i widełkach płacowych na podstawie zebranych danych historycznych.