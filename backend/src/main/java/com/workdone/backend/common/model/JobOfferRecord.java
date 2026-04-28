package com.workdone.backend.common.model;

import lombok.Builder;
import lombok.With;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Mój główny rekord reprezentujący ofertę pracy w całym systemie. 
 * Niezmienny (Immutable), z builderem i metodami @With do łatwej aktualizacji.
 */
@With
@Builder(toBuilder = true)
public record JobOfferRecord(
        String id,
        String fingerprint,   // Unikalny skrót treści (tytuł+firma), żeby wyłapać te same oferty z różnych stron
        String title,
        String companyName,
        String sourceUrl,     // Adres do ogłoszenia (wymagany)
        String location,
        String rawDescription,
        String salaryRange,
        List<String> techStack,
        Double matchingScore, // Wynik dopasowania do mojego CV (0-100)
        Double priorityScore, // Finalny priorytet po uwzględnieniu boostów
        OfferStatus status,   // Status (NEW, ANALYZED, APPLIED, itp.)
        LocalDateTime publishedAt,
        String sourcePlatform // Skąd pobrano (np. JOOBLE, JOBICY)
) {
    public JobOfferRecord {
        // sourceUrl to jedyne pole, bez którego system nie będzie działał (nie da się sprawdzić szczegółów ani zaaplikować)
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException("Oferta musi posiadać unikalny adres URL (sourceUrl)");
        }
    }
}
