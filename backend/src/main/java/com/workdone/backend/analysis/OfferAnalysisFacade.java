package com.workdone.backend.analysis;

import com.workdone.backend.format.OfferContentBuilder;
import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.storage.OfferVectorStore;
import org.springframework.stereotype.Service;

@Service
public class OfferAnalysisFacade {

    private final OfferMatchingService matchingService;
    private final OfferScoringService scoringService;
    private final OfferClassificationService classificationService;
    private final OfferVectorStore vectorStore;
    private final OfferContentBuilder contentBuilder;
    private final OfferPriorityService priorityService;

    public OfferAnalysisFacade(OfferMatchingService matchingService,
                               OfferScoringService scoringService,
                               OfferClassificationService classificationService,
                               OfferVectorStore vectorStore,
                               OfferContentBuilder contentBuilder,
                               OfferPriorityService priorityService) {
        this.matchingService = matchingService;
        this.scoringService = scoringService;
        this.classificationService = classificationService;
        this.vectorStore = vectorStore;
        this.contentBuilder = contentBuilder;
        this.priorityService = priorityService;
    }

    public JobOfferRecord analyze(JobOfferRecord offer) {
        String content = contentBuilder.buildTechnicalContent(offer);

        vectorStore.save(offer, content);

        double baseScore;
        try {
            baseScore = scoringService.score(offer).score();
        } catch (Exception e) {
            baseScore = matchingService.quickScore(offer);
        }

        double priority = priorityService.calculate(
                offer.withMatchingScore(baseScore)
        );

        MatchingBand band = classificationService.classify(priority);

        return offer
                .withMatchingScore(baseScore)
                .withPriorityScore(priority)
                .withStatus(classificationService.toStatus(band));
    }
}