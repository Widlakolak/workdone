package com.workdone.backend.common.util;

import com.workdone.backend.common.model.JobOfferRecord;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Fabryka tekstu. Formatuje surowe dane o ofertach na ładne wiadomości, 
 * które potem lądują na moim Discordzie.
 */
@Component
public class OfferContentBuilder {

    /**
     * Buduje skróconą, "techniczną" treść oferty do porównania wektorowego. 
     * Skupiam się na kluczowych polach: tytuł, firma, miejsce i opis.
     */
    public String buildTechnicalContent(JobOfferRecord offer) {
        // Dodanie etykiet (Role, Company itp.) pomaga modelowi lepiej "zrozumieć" strukturę.
        // Używamy średnika jako separatora, który jest dobrze rozumiany przez tokenizery.
        return String.format("Role: %s; Company: %s; Location: %s; Description: %s",
                normalize(offer.title()),
                normalize(offer.companyName()),
                normalize(offer.location()),
                cleanDescription(offer.rawDescription())
        );
    }

    private String normalize(String input) {
        if (input == null) return "";
        return input.trim().replaceAll("\\s+", " ");
    }

    private String cleanDescription(String description) {
        if (description == null) return "";
        // Opcjonalnie: Usuwanie tagów HTML, jeśli mogą się pojawić
        String noHtml = description.replaceAll("<[^>]*>", " ");
        return normalize(noHtml);
    }

    /**
     * Przygotowuje treść wiadomości dla ofert, które wymagają mojej natychmiastowej uwagi.
     */
    public String buildInstantMessage(JobOfferRecord offer) {
        return """
               🔥 **NOWA DOBRA OFERTA!**
               **%s** @ **%s**
               Dopasowanie: %s%%
               Priorytet: %s%%
               Link: %s
               """.formatted(
                offer.title(),
                offer.companyName(),
                formatScore(offer.matchingScore()),
                formatScore(offer.priorityScore()),
                offer.sourceUrl()
        ).trim();
    }

    /**
     * Składa listę "całkiem niezłych" ofert w jedno zbiorcze podsumowanie. 
     * Sortuję je od najwyższego priorytetu i ograniczam do 20 sztuk, żeby nie robić spamu.
     */
    public String buildDigestMessage(List<JobOfferRecord> offers) {
        StringBuilder message = new StringBuilder("📋 **ZBIORCZY RAPORT DZIENNY**\n");
        offers.stream()
                .sorted((a, b) -> Double.compare(safeValue(b.priorityScore()), safeValue(a.priorityScore())))
                .limit(20)
                .forEach(offer -> message.append(String.format(
                        " [Match: %d%% | Prio: %d%%]\n- %s @ %s\n<%s>\n",
                        Math.round(safeValue(offer.matchingScore())),
                        Math.round(safeValue(offer.priorityScore())),
                        offer.title(),
                        offer.companyName(),
                        offer.sourceUrl()
                )));
        return message.toString();
    }

    private String formatScore(Double score) {
        return score == null ? "-" : String.valueOf(Math.round(score));
    }

    private double safeValue(Double value) {
        return value == null ? 0.0 : value;
    }
}
