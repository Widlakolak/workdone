package com.workdone.backend.analysis;

import com.workdone.backend.format.OfferContentBuilder;
import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.model.OfferStatus;
import com.workdone.backend.storage.OfferVectorStore;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OfferScoringFacadeTest {

    @Test
    void shouldNotRunDeepAiWhenThresholdIsConfiguredAsFraction() {
        OfferMatchingService matchingService = Mockito.mock(OfferMatchingService.class);
        OfferEmbeddingService embeddingService = Mockito.mock(OfferEmbeddingService.class);
        OfferVectorStore vectorStore = Mockito.mock(OfferVectorStore.class);
        DynamicConfigService dynamicConfig = Mockito.mock(DynamicConfigService.class);
        OfferAnalysisFacade aiAnalysisFacade = Mockito.mock(OfferAnalysisFacade.class);
        OfferPriorityService priorityService = Mockito.mock(OfferPriorityService.class);
        OfferClassificationService classificationService = Mockito.mock(OfferClassificationService.class);
        OfferContentBuilder contentBuilder = Mockito.mock(OfferContentBuilder.class);

        OfferScoringFacade facade = new OfferScoringFacade(
                matchingService, embeddingService, vectorStore, dynamicConfig,
                aiAnalysisFacade, priorityService, classificationService, contentBuilder
        );

        JobOfferRecord offer = JobOfferRecord.builder()
                .id("1")
                .fingerprint("fp")
                .title("Java Developer")
                .companyName("ACME")
                .sourceUrl("https://example.com/job/1")
                .location("Remote")
                .rawDescription("Desc")
                .salaryRange("n/a")
                .techStack(List.of("Java"))
                .matchingScore(0.0)
                .priorityScore(0.0)
                .status(OfferStatus.NEW)
                .publishedAt(LocalDateTime.now())
                .sourcePlatform("TEST")
                .build();

        Mockito.when(matchingService.passesMustHave(offer)).thenReturn(true);
        Mockito.when(contentBuilder.buildTechnicalContent(offer)).thenReturn("content");
        Mockito.when(vectorStore.calculateCosineSimilarity(Mockito.any(), Mockito.any())).thenReturn(0.10);
        Mockito.when(dynamicConfig.getSemanticThreshold()).thenReturn(0.7);
        Mockito.when(priorityService.calculate(Mockito.any())).thenReturn(10.0);
        Mockito.when(classificationService.classify(10.0)).thenReturn(MatchingBand.ARCHIVED);
        Mockito.when(classificationService.toStatus(MatchingBand.ARCHIVED)).thenReturn(OfferStatus.ANALYZED);

        OfferScoringFacade.ScoringAnalysis result = facade.score(
                offer,
                new float[] {0.1f, 0.2f},
                new float[] {0.1f, 0.2f}
        );

        Mockito.verify(aiAnalysisFacade, Mockito.never()).performDeepAnalysis(offer);
        assertThat(result.isRejected()).isFalse();
        assertThat(result.baseScore()).isEqualTo(10.0);
    }
}