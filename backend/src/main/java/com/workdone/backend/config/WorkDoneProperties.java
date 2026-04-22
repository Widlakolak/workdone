package com.workdone.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "workdone")
public record WorkDoneProperties(
        Profile profile,
        Matching matching,
        Scheduling scheduling,
        Discord discord,
        Providers providers,
        Search search // Sekcja z globalnymi ustawieniami wyszukiwania
) {

    // Konfiguracja ścieżki do folderu z CV-kami
    public record Profile(String inputDirectory) {
    }

    // Progi punktowe dla ofert i słowa kluczowe "must-have"
    public record Matching(
            double instantThreshold, // Powyżej tego progu oferta idzie od razu na Discorda
            double digestThreshold,  // Powyżej tego progu oferta idzie do dziennego podsumowania
            double archiveThreshold, // Poniżej tego progu oferta idzie do archiwum
            double semanticThreshold, // Od tego progu odpalamy głęboką analizę AI
            List<String> mustHaveKeywords, // Słowa kluczowe, które MUSZĄ być w ofercie
            double aiThresholdMin,   // Minimalny wynik AI, żeby oferta była brana pod uwagę
            double aiThresholdMax    // Maksymalny wynik AI (górne ograniczenie)
    ) {}

    // Ustawienia harmonogramu dla pobierania ofert i generowania podsumowań
    public record Scheduling(String ingestionCron, String digestCron, String zoneId) {
    }

    // Konfiguracja webhooków Discorda
    public record Discord(Webhook instant, Webhook digest) {
    }

    // Szczegóły webhooka Discorda (czy włączony, URL)
    public record Webhook(boolean enabled, String url) {
    }

    // Konfiguracja dostawców ofert (np. Jooble, Jobicy, RSS)
    public record Providers(
            ProviderConfig jooble
    ) {}

    // Ogólna konfiguracja dla każdego dostawcy ofert
    public record ProviderConfig(
            boolean enabled,        // Czy dostawca jest włączony
            String url,             // Bazowy URL API
            String priority,        // Priorytet (np. "high", "medium", "low")
            Filters filters         // Specyficzne filtry dla tego dostawcy
    ) {}

    // Filtry, które można zastosować do zapytań do dostawców ofert
    public record Filters(
            List<String> keywords,      // Dodatkowe słowa kluczowe
            List<String> allowedCities, // Dozwolone miasta
            Boolean allowRemote,        // Czy szukać ofert zdalnych
            Boolean allowHybrid,        // Czy szukać ofert hybrydowych
            String query,               // Dowolny string zapytania
            Integer minBudgetUsd        // Minimalny budżet w USD
    ) {}

    // Globalne ustawienia wyszukiwania ofert
    public record Search(
            String defaultLocation, // Domyślna lokalizacja do wyszukiwania
            boolean allowRemote     // Czy domyślnie zezwalać na wyszukiwanie zdalne
    ) {}
}
