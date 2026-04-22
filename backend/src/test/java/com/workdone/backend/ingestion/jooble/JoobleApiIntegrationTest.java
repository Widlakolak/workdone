package com.workdone.backend.ingestion.jooble;

import com.workdone.backend.analysis.*;
import com.workdone.backend.profile.parser.CvSemanticParser;
import com.workdone.backend.profile.service.CandidateProfileService;
import com.workdone.backend.profile.service.CvAggregationService;
import com.workdone.backend.analysis.DynamicConfigService;
import com.workdone.backend.storage.OfferVectorStore;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(properties = {
        "workdone.providers.jooble.apiKey=${JOOBLE_API_KEY}",
        "workdone.providers.jooble.url=https://jooble.org/api/",
        "workdone.providers.jooble.enabled=true",
        "workdone.providers.jooble.filters.keywords=java",
        "workdone.providers.jooble.filters.location=Europe"
})
@ActiveProfiles("test")
class JoobleApiIntegrationTest {

    @Autowired
    private JoobleJobProvider joobleJobProvider;

    @MockitoBean private OfferAnalysisFacade offerAnalysisFacade;
    @MockitoBean private OfferDeduplicationService offerDeduplicationService;
    @MockitoBean private OfferScoringService offerScoringService;
    @MockitoBean private CvAggregationService cvAggregationService;
    @MockitoBean private CvSemanticParser cvSemanticParser;
    @MockitoBean private OfferVectorStore offerVectorStore;
    @MockitoBean private CandidateProfileService candidateProfileService;
    @MockitoBean private OfferEmbeddingService offerEmbeddingService;
    @MockitoBean private EmbeddingModel embeddingModel;
    @MockitoBean private DynamicConfigService dynamicConfigService;

    @Test
    void checkActualJoobleResponse() {
        var offers = joobleJobProvider.fetchOffers();
        assertThat(offers).isNotNull();
    }
}
