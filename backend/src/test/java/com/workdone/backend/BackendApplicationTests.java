package com.workdone.backend;

import com.workdone.backend.analysis.*;
import com.workdone.backend.orchestration.OfferIngestionOrchestrator;
import com.workdone.backend.profile.parser.CvSemanticParser;
import com.workdone.backend.profile.service.CandidateProfileService;
import com.workdone.backend.profile.service.CvAggregationService;
import com.workdone.backend.analysis.DynamicConfigService;
import com.workdone.backend.storage.OfferVectorStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {

    // Mockujemy wszystko co mogłoby wybuchnąć przy starcie
    @MockitoBean private OfferIngestionOrchestrator orcherstrator;
    @MockitoBean private CandidateProfileService profileService;
    @MockitoBean private OfferScoringService scoringService;
    @MockitoBean private OfferEmbeddingService embeddingService;
    @MockitoBean private CvAggregationService aggregationService;
    @MockitoBean private CvSemanticParser semanticParser;
    @MockitoBean private OfferVectorStore vectorStore;
    @MockitoBean private DynamicConfigService configService;

    @Test
    void contextLoads() {
    }
}
