package com.workdone.backend.profile.service;

import com.workdone.backend.analysis.DynamicConfigService;
import com.workdone.backend.analysis.OfferEmbeddingService;
import com.workdone.backend.profile.parser.CvSemanticParser;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CandidateProfileServiceTest {

    @Test
    void shouldKeepVectorWhenSemanticParserFails() {
        CvAggregationService cvAggregationService = Mockito.mock(CvAggregationService.class);
        OfferEmbeddingService embeddingService = Mockito.mock(OfferEmbeddingService.class);
        CvSemanticParser cvSemanticParser = Mockito.mock(CvSemanticParser.class);
        Environment environment = Mockito.mock(Environment.class);
        DynamicConfigService dynamicConfigService = Mockito.mock(DynamicConfigService.class);

        CandidateProfileService service = new CandidateProfileService(
                cvAggregationService,
                embeddingService,
                cvSemanticParser,
                environment,
                dynamicConfigService
        );

        Mockito.when(cvAggregationService.buildMergedProfile()).thenReturn("java spring profile");
        Mockito.when(embeddingService.embed("java spring profile")).thenReturn(new float[] {0.1f, 0.2f});
        Mockito.when(cvSemanticParser.parse("java spring profile")).thenThrow(new RuntimeException("quota"));

        boolean refreshed = service.refreshProfile();

        assertThat(refreshed).isTrue();
        assertThat(service.getCandidateVector()).containsExactly(0.1f, 0.2f);
        assertThat(service.getLatestProfileText()).isEqualTo("java spring profile");
        assertThat(service.getSuggestedKeywords()).isEqualTo(List.of());
        Mockito.verify(dynamicConfigService, Mockito.never()).syncWithProfile();
    }
}