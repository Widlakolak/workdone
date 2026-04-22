package com.workdone.backend.analysis;

import com.workdone.backend.format.OfferContentBuilder;
import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.storage.OfferVectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferScoringFacade {

    private final OfferMatchingService matchingService;
    private final OfferEmbeddingService embeddingService;
    private final OfferVectorStore vectorStore;
    private final DynamicConfigService dynamicConfig;
    private final OfferAnalysisFacade aiAnalysisFacade;
    private final OfferPriorityService priorityService;
    private final OfferClassificationService classificationService;
    private final OfferContentBuilder contentBuilder;

    /**
     * Główna metoda scoringu. Przyjmuje opcjonalny offerVector, aby nie liczyć go dwa razy.
     */
    public ScoringAnalysis score(JobOfferRecord offer, float[] candidateVector, float[] offerVector) {
        // Na początek sprawdzam, czy oferta w ogóle spełnia moje minimalne wymagania (Must-Have)
        if (!matchingService.passesMustHave(offer)) {
            return ScoringAnalysis.rejectedResult();
        }

        // Buduję tekst techniczny oferty do porównania z moim CV
        String technicalContent = contentBuilder.buildTechnicalContent(offer);
        
        // Jeśli nie dostaliśmy wektora z zewnątrz, to musimy go tu wygenerować
        if (offerVector == null) {
            try {
                offerVector = embeddingService.embed(technicalContent);
            } catch (Exception e) {
                log.error("❌ Embedding failed: {}", e.getMessage());
                return ScoringAnalysis.rejectedResult();
            }
        }

        double semanticScore = 0.0;
        try {
            semanticScore = vectorStore.calculateCosineSimilarity(candidateVector, offerVector) * 100;
        } catch (Exception e) {
            log.error("❌ Similarity calculation failed: {}", e.getMessage());
        }
        
        double baseScore = semanticScore;
        // Jeśli podobieństwo semantyczne jest obiecujące, odpalam ciężką artylerię - analizę przez LLM
        // Tu sprawdzam czy to nie duplikat, żeby nie bulić za AI (notatka: to jest ten drogi krok LLM)
        if (semanticScore >= dynamicConfig.getSemanticThreshold()) {
            Double aiScore = aiAnalysisFacade.performDeepAnalysis(offer);
            if (aiScore != null) {
                baseScore = aiScore;
            }
        }

        // Obliczam finalny priorytet (uwzględniając boosty za lokalizację, junior-friendly itp.)
        double finalPriority = priorityService.calculate(offer.withMatchingScore(baseScore));
        MatchingBand band = classificationService.classify(finalPriority);

        return ScoringAnalysis.builder()
                .baseScore(baseScore)
                .finalPriority(finalPriority)
                .band(band)
                .status(classificationService.toStatus(band))
                .technicalContent(technicalContent)
                .offerVector(offerVector)
                .isRejected(false)
                .build();
    }

    @lombok.Builder
    public record ScoringAnalysis(
            double baseScore,
            double finalPriority,
            MatchingBand band,
            com.workdone.backend.model.OfferStatus status,
            String technicalContent,
            float[] offerVector,
            boolean isRejected
    ) {
        public static ScoringAnalysis rejectedResult() {
            return ScoringAnalysis.builder().isRejected(true).build();
        }
    }
}
