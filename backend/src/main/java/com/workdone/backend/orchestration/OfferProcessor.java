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
import com.workdone.backend.storage.OfferVectorStore; // Import OfferVectorStore
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mózg operacyjny procesu pojedynczej oferty. 
 * Tu zapadają decyzje: czy oferta mi pasuje, czy jest duplikatem i czy wysłać alert na Discorda.
 */
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
    private final OfferVectorStore vectorStore; // Dodaję OfferVectorStore

    /**
     * Przetwarzam pojedynczą ofertę w nowej transakcji. 
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void processOffer(JobOfferRecord offer, float[] candidateVector) {
        
        // 1. Wyciągam technicalContent - muszę to mieć przed embeddingiem
        String technicalContent = contentBuilder.buildTechnicalContent(offer);

        // 2. Generuję wektor (Single Embedding Pattern)
        // Tu sprawdzam czy to nie duplikat, żeby nie bulić za AI (notatka: embedding kosztuje grosze, LLM dużo więcej)
        float[] offerVector = null;
        try {
            offerVector = embeddingService.embed(technicalContent);
        } catch (Exception e) {
            log.error("❌ Błąd embeddingu dla {}: {}", offer.title(), e.getMessage());
            return;
        }

        // 3. Sprawdzam duplikaty ZANIM odpalę scoring (czyli ZANIM odpalę LLM)
        if (deduplicationService.isDuplicate(offerVector)) {
            log.debug("⏭️ To już było (duplikat treści): {}", offer.title());
            return;
        }

        // 4. Zapisuję ofertę do bazy wektorowej, bo jest unikalna.
        // Tu zapisuję do bazy wektorowej, żeby już nigdy nie bulić za embedding tej samej oferty.
        vectorStore.save(offer, technicalContent);

        // 5. Dopiero teraz odpalam scoring (tu może polecieć LLM, ale wiemy że oferta jest unikalna)
        ScoringAnalysis analysis = scoringFacade.score(offer, candidateVector, offerVector);
        
        if (analysis.isRejected()) {
            return;
        }

        log.info("⚖️ ANALIZA: {} | DOPASOWANIE: {}%", offer.title(), String.format("%.1f", analysis.baseScore()));
        log.info("🏷️ WYNIK: {} (Priorytet: {}) -> STATUS: {}", analysis.band(), String.format("%.1f", analysis.finalPriority()), analysis.status());

        // 6. Wzbogacam rekord o wyniki analizy
        JobOfferRecord enriched = offer.toBuilder()
                .matchingScore(analysis.baseScore())
                .priorityScore(analysis.finalPriority())
                .status(analysis.status())
                .build();

        // 7. Zapisuję do bazy SQL
        store.upsert(enriched);

        // 8. Jak oferta to sztos (INSTANT), to od razu lecę z powiadomieniem na Discorda
        if (analysis.band() == MatchingBand.INSTANT) {
            notifier.sendInstant(enriched);
        }
    }
}
