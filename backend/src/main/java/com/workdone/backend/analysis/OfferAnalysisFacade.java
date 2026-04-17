package com.workdone.backend.analysis;

import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.storage.OfferVectorStore;
import org.springframework.stereotype.Service;

@Service
public class OfferAnalysisFacade {

    private final OfferMatchingService matchingService;
    private final OfferScoringService scoringService;
    private final OfferEmbeddingService embeddingService;
    private final OfferClassificationService classificationService;
    private final OfferVectorStore vectorStore;
    private final OfferDeduplicationService deduplicationService;

    public OfferAnalysisFacade(OfferMatchingService matchingService,
                               OfferScoringService scoringService,
                               OfferEmbeddingService embeddingService,
                               OfferClassificationService classificationService,
                               OfferVectorStore vectorStore,
                               OfferDeduplicationService deduplicationService) {
        this.matchingService = matchingService;
        this.scoringService = scoringService;
        this.embeddingService = embeddingService;
        this.classificationService = classificationService;
        this.vectorStore = vectorStore;
        this.deduplicationService = deduplicationService;
    }

    public JobOfferRecord analyze(JobOfferRecord offer) {

        String content = buildContent(offer);

        // 1. MUST HAVE (cheap)
        if (!matchingService.passesMustHave(offer)) {
            return offer;
        }

        // 2. SEMANTIC DEDUPE (PRZED zapisem!)
        if (deduplicationService.isDuplicate(content)) {
            return offer;
        }

        // 3. ZAPIS DO VECTOR DB (po dedupe)
        vectorStore.save(offer, content);

        OfferScoringResult scoringResult;
        double score;

        // 4. AI scoring + fallback
        try {
            scoringResult = scoringService.score(offer);
            score = scoringResult.score();
        } catch (Exception e) {
            score = matchingService.score(offer);
            scoringResult = new OfferScoringResult(score, true, "Fallback local scoring");
        }

        // 5. klasyfikacja
        MatchingBand band = classificationService.classify(score);

        return offer.withAnalysis(score, classificationService.toStatus(band));
    }

    private String buildContent(JobOfferRecord offer) {
        return """
            title: %s
            company: %s
            location: %s
            description: %s
            """.formatted(
                offer.title(),
                offer.companyName(),
                offer.location(),
                offer.rawDescription()
        );
    }
}