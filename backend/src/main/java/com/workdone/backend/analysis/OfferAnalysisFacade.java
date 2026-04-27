package com.workdone.backend.analysis;

import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.notification.DiscordNotifier;
import com.workdone.backend.storage.AiAnalysisCacheStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferAnalysisFacade {

    private final OfferScoringService scoringService;
    private final DiscordNotifier discordNotifier;
    private final AiExecutionPolicy aiPolicy;
    private final AiAnalysisCacheStore aiCache;

    public enum AnalysisSource { AI, CACHE, SKIPPED }
    public record AnalysisResponse(Double score, AnalysisSource source) {}

    public AnalysisResponse performDeepAnalysis(JobOfferRecord offer, double semanticScore) {
        // 1. DB Cache Lookup - trwała pamięć
        var cached = aiCache.get(offer.fingerprint());
        if (cached.isPresent()) {
            log.info("🎯 [DB CACHE HIT] {} -> {}%", offer.title(), cached.get().score());
            return new AnalysisResponse(cached.get().score(), AnalysisSource.CACHE);
        }

        // 2. Budget & Policy Check
        if (!aiPolicy.canExecute(semanticScore)) {
            log.info("⏭️ [SKIP AI] {} (Semantyka: {}%, Pozostały budżet: {})",
                    offer.title(), String.format("%.1f", semanticScore), aiPolicy.getRemainingBudget());
            return new AnalysisResponse(null, AnalysisSource.SKIPPED);
        }

        log.info("🧠 [AI START] Analiza deep dla: {} (Dopasowanie: {}%)", offer.title(), String.format("%.1f", semanticScore));

        try {
            var result = scoringService.score(offer);
            if (result != null) {
                // Zapisujemy do DB, żeby nigdy więcej nie pytać o ten fingerprint
                aiCache.put(offer.fingerprint(), result.score(), result.reasoning(), "MultiModel-Aggregator");
                aiPolicy.recordExecution();
                log.info("✅ [AI SUCCESS] Wynik: {} dla {}", result.score(), offer.title());
                return new AnalysisResponse(result.score(), AnalysisSource.AI);
            }
        } catch (Exception e) {
            String error = "❌ AI failed for " + offer.title() + ": " + e.getMessage();
            log.warn(error);
            discordNotifier.sendAiAlert(error);
        }

        return new AnalysisResponse(null, AnalysisSource.SKIPPED);
    }
}