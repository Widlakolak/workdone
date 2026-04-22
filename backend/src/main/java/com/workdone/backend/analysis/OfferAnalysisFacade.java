package com.workdone.backend.analysis;

import com.workdone.backend.format.OfferContentBuilder;
import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.notification.DiscordNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferAnalysisFacade {

    private final OfferScoringService scoringService;
    private final OfferContentBuilder contentBuilder;
    private final DiscordNotifier discordNotifier; // Wstrzykujemy DiscordNotifier

    /**
     * Odpalam LLM-a do głębokiej analizy.
     * Zwracam wynik punktowy od AI (0-100).
     */
    public Double performDeepAnalysis(JobOfferRecord offer) {
        log.info("Starting deep AI analysis for: {}", offer.title());
        
        String content = contentBuilder.buildTechnicalContent(offer);

        try {
            return scoringService.score(offer).score();
        } catch (Exception e) {
            String errorMessage = String.format("❌ Deep AI scoring failed for offer '%s', error: %s", offer.title(), e.getMessage());
            log.warn(errorMessage, e);
            discordNotifier.sendAiAlert(errorMessage); // Wysyłamy alert na Discorda
            // Jeśli AI padnie (np. timeout), zwracam null, żeby orkiestrator wiedział, że ma użyć podobieństwa cosinusowego jako fallbacku
            return null;
        }
    }
}
