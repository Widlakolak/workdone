package com.workdone.backend.joboffer.orchestration;

import com.workdone.backend.joboffer.analysis.*;
import com.workdone.backend.joboffer.analysis.OfferAnalysisFacade.AnalysisResponse;
import com.workdone.backend.joboffer.analysis.OfferAnalysisFacade.AnalysisSource;
import com.workdone.backend.common.model.JobOfferRecord;
import com.workdone.backend.common.model.OfferStatus;
import com.workdone.backend.joboffer.notification.DiscordNotifier;
import com.workdone.backend.joboffer.storage.OfferStore;
import com.workdone.backend.joboffer.storage.OfferVectorStore;
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
    private final DynamicConfigService dynamicConfig;

    public ProcessingResult preProcess(JobOfferRecord offer, float[] candidateVector, float[] offerVector) {
        double semanticScore = 0;
        if (candidateVector != null && offerVector != null) {
            semanticScore = vectorStore.calculateCosineSimilarity(candidateVector, offerVector) * 100;
        }

        boolean strictMustHave = matchingService.passesMustHave(offer);
        boolean coreMustHave = matchingService.passesCoreMustHave(offer);
        double semanticThreshold = dynamicConfig.getSemanticThreshold();

        // Staged filtering:
        // 1) jeśli spełnia pełne must-have -> przechodzi
        // 2) jeśli nie spełnia pełnego must-have, ale spełnia core (language + framework) -> przechodzi
        // 3) jeśli nie spełnia core, przepuszczamy tylko gdy semantyka jest >= configured threshold (AI rescue lane)
        if (!strictMustHave && !coreMustHave && semanticScore < semanticThreshold) {
            return ProcessingResult.skipped();
        }

        // Kara za "rescue lane", żeby pełne dopasowania miały priorytet
        if (!strictMustHave) {
            semanticScore *= 0.90;
        }

        JobOfferRecord enriched = offer.toBuilder()
                .matchingScore(semanticScore)
                .status(OfferStatus.NEW)
                .build();

        try {
            store.upsert(enriched);
            log.debug("💾 [STAGING] Zapisano ofertę do bazy: {}", enriched.title());
        } catch (Exception e) {
            log.error("❌ Błąd zapisu stagingu dla {}: {}", enriched.title(), e.getMessage());
        }

        return new ProcessingResult(enriched, null, true, null, offerVector);
    }

    public ProcessingResult enrichWithAi(JobOfferRecord offer, float[] candidateVector, float[] offerVector) {
        AnalysisResponse response = aiAnalysisFacade.performDeepAnalysis(offer, offer.matchingScore());

        double finalScore = (response.score() != null) ? response.score() : offer.matchingScore();
        double priority = priorityService.calculate(offer.withMatchingScore(finalScore));
        MatchingBand band = classificationService.classify(priority);

        JobOfferRecord finalOffer = offer.toBuilder()
                .matchingScore(finalScore)
                .priorityScore(priority)
                .status(classificationService.toStatus(band))
                .build();

        store.upsert(finalOffer);

        if (band == MatchingBand.INSTANT) {
            notifier.sendInstant(finalOffer);
        }

        return new ProcessingResult(finalOffer, band, true, response.source(), offerVector);
    }

    public record ProcessingResult(
            JobOfferRecord offer,
            MatchingBand band,
            boolean processed,
            AnalysisSource source,
            float[] vector
    ) {
        public static ProcessingResult skipped() {
            return new ProcessingResult(null, null, false, null, null);
        }
    }
}