package com.workdone.backend.orchestration;

import com.workdone.backend.analysis.MatchingBand;
import com.workdone.backend.analysis.OfferDeduplicationService;
import com.workdone.backend.analysis.OfferEmbeddingService;
import com.workdone.backend.analysis.OfferScoringFacade;
import com.workdone.backend.analysis.OfferScoringFacade.ScoringAnalysis;
import com.workdone.backend.format.OfferContentBuilder;
import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.notification.DiscordNotifier;
import com.workdone.backend.storage.OfferStore;
import com.workdone.backend.storage.OfferVectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferProcessor {

    private final OfferDeduplicationService deduplicationService;
    private final OfferScoringFacade scoringFacade;
    private final OfferStore store;
    private final DiscordNotifier notifier;
    private final OfferContentBuilder contentBuilder;
    private final OfferEmbeddingService embeddingService;
    private final OfferVectorStore vectorStore;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public ProcessingResult processOffer(JobOfferRecord offer, float[] candidateVector) {
        return processOffer(offer, candidateVector, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public ProcessingResult processOffer(JobOfferRecord offer, float[] candidateVector, float[] precomputedVector) {

        String technicalContent = contentBuilder.buildTechnicalContent(offer);
        float[] offerVector = precomputedVector;

        if (offerVector == null) {
            try {
                offerVector = embeddingService.embed(technicalContent);
            } catch (Exception e) {
                log.error("❌ Błąd embeddingu dla {}: {}", offer.title(), e.getMessage());
                return ProcessingResult.skipped();
            }
        }

        if (deduplicationService.isDuplicate(offerVector)) {
            log.debug("⏭️ Duplikat treści: {}", offer.title());
            return ProcessingResult.skipped();
        }

        vectorStore.save(offer, technicalContent);

        ScoringAnalysis analysis = scoringFacade.score(offer, candidateVector, offerVector);
        
        if (analysis.isRejected()) {
            return ProcessingResult.skipped();
        }

        log.info("⚖️ ANALIZA: {} | DOPASOWANIE: {}%", offer.title(), String.format("%.1f", analysis.baseScore()));

        JobOfferRecord enriched = offer.toBuilder()
                .matchingScore(analysis.baseScore())
                .priorityScore(analysis.finalPriority())
                .status(analysis.status())
                .build();

        store.upsert(enriched);

        if (analysis.band() == MatchingBand.INSTANT) {
            notifier.sendInstant(enriched);
        }

        return new ProcessingResult(enriched, analysis.band(), true);
    }

    public record ProcessingResult(JobOfferRecord offer, MatchingBand band, boolean processed) {
        public static ProcessingResult skipped() {
            return new ProcessingResult(null, null, false);
        }
    }
}
