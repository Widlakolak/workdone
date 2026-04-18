package com.workdone.backend;

import com.workdone.backend.analysis.OfferAnalysisFacade;
import com.workdone.backend.analysis.OfferDeduplicationService;
import com.workdone.backend.analysis.OfferMatchingService;
import com.workdone.backend.analysis.OfferScoringService;
import com.workdone.backend.profile.parser.CvSemanticParser;
import com.workdone.backend.profile.service.CvAggregationService;
import com.workdone.backend.storage.OfferVectorStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
        org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration.class,
        org.springframework.ai.model.google.genai.autoconfigure.embedding.GoogleGenAiEmbeddingConnectionAutoConfiguration.class,
        org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration.class
})
class BackendApplicationTests {

    @MockitoBean
    private OfferAnalysisFacade offerAnalysisFacade;

    @MockitoBean
    private OfferDeduplicationService offerDeduplicationService;

    @MockitoBean
    private OfferMatchingService offerMatchingService;

    @MockitoBean
    private OfferScoringService offerScoringService;

    @MockitoBean
    private CvAggregationService cvAggregationService;

    @MockitoBean
    private CvSemanticParser cvSemanticParser;

    @MockitoBean
    private OfferVectorStore offerVectorStore;

    @Test
    void contextLoads() {
    }
}