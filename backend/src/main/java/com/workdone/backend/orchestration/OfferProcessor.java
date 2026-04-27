package com.workdone.backend.orchestration;

import com.workdone.backend.analysis.*;
import com.workdone.backend.analysis.OfferAnalysisFacade.AnalysisResponse;
import com.workdone.backend.analysis.OfferAnalysisFacade.AnalysisSource;
import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.model.OfferStatus;
import com.workdone.backend.notification.DiscordNotifier;
import com.workdone.backend.storage.OfferStore;
import com.workdone.backend.storage.OfferVectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferProcessor {

    private final OfferStore store;
    private final DiscordNotifier notifier;
    private final OfferVectorStore vectorStore;
    private final OfferClassificationService classificationService;
    private final OfferPriorityService priorityService;
    private final OfferAnalysisFacade aiAnalysisFacade;
    private final OfferMatchingService matchingService;

    // Faza 1
    public ProcessingResult preProcess(JobOfferRecord offer, float[] candidateVector, float[] offerVector) {
        // 1. Must-Have Check
        if (!matchingService.passesMustHave(offer)) {
            return ProcessingResult.skipped();
        }

        // 2. Szybki scoring semantyczny
        double semanticScore = 0;
        if (candidateVector != null && offerVector != null) {
            semanticScore = vectorStore.calculateCosineSimilarity(candidateVector, offerVector) * 100;
        }

        // Rekord ze statusem NEW
        JobOfferRecord enriched = offer.toBuilder()
                .matchingScore(semanticScore)
                .status(OfferStatus.NEW)
                .build();

        // 3: Zapisujemy każdą ofertę, która przeszła Must-Have
        try {
            store.upsert(enriched);
            log.debug("💾 [STAGING] Zapisano ofertę do bazy: {}", enriched.title());
        } catch (Exception e) {
            log.error("❌ Błąd zapisu stagingu dla {}: {}", enriched.title(), e.getMessage());
        }

        return new ProcessingResult(enriched, null, true, null);
    }

    // Faza 2: Głęboka analiza (AI) + Klasyfikacja + Powiadomienie
    public ProcessingResult enrichWithAi(JobOfferRecord offer, float[] candidateVector) {
        AnalysisResponse response = aiAnalysisFacade.performDeepAnalysis(offer, offer.matchingScore());

        double finalScore = (response.score() != null) ? response.score() : offer.matchingScore();
        double priority = priorityService.calculate(offer.withMatchingScore(finalScore));
        MatchingBand band = classificationService.classify(priority);

        JobOfferRecord finalOffer = offer.toBuilder()
                .matchingScore(finalScore)
                .priorityScore(priority)
                .status(classificationService.toStatus(band))
                .build();

        // Zapisujemy ofertę dopiero gdy ma sensowny status
        store.upsert(finalOffer);

        if (band == MatchingBand.INSTANT) {
            notifier.sendInstant(finalOffer);
        }

        return new ProcessingResult(finalOffer, band, true, response.source());
    }

    public record ProcessingResult(
            JobOfferRecord offer,
            MatchingBand band,
            boolean processed,
            AnalysisSource source
    ) {
        public static ProcessingResult skipped() {
            return new ProcessingResult(null, null, false, null);
        }
    }
}