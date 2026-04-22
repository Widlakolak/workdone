package com.workdone.backend.orchestration;

import com.workdone.backend.analysis.*;
import com.workdone.backend.profile.parser.CvSemanticParser;
import com.workdone.backend.profile.service.CandidateProfileService;
import com.workdone.backend.profile.service.CvAggregationService;
import com.workdone.backend.analysis.DynamicConfigService;
import com.workdone.backend.storage.InMemoryOfferStore;
import com.workdone.backend.storage.OfferStore;
import com.workdone.backend.storage.OfferVectorStore;
import com.workdone.backend.storage.PersistentOfferStore;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OfferIngestionOrchestratorTest {

    @Autowired
    private OfferStore offerStore;

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
    void shouldLoadInMemoryStoreInTestProfile() {
        assertThat(offerStore).isInstanceOf(InMemoryOfferStore.class);
        assertThat(offerStore).isNotInstanceOf(PersistentOfferStore.class);
    }
}
