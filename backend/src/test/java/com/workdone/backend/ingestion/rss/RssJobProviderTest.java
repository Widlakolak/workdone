package com.workdone.backend.ingestion.rss;

import com.workdone.backend.ingestion.SearchContext;
import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.profile.service.CandidateProfileService;
import com.workdone.backend.analysis.DynamicConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.workdone.backend.analysis.*;
import com.workdone.backend.profile.parser.CvSemanticParser;
import com.workdone.backend.profile.service.CvAggregationService;
import com.workdone.backend.storage.OfferVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RssJobProviderTest {

    @Autowired
    private RssJobProvider rssJobProvider;

    // Mockujemy zależności AI, aby test nie wymagał kluczy API
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
    void shouldFetchOffersFromRealRssFeeds() {
        // GIVEN
        SearchContext context = SearchContext.builder()
                .keywords(List.of("java"))
                .location("poland")
                .remoteOnly(true)
                .build();

        // WHEN
        List<JobOfferRecord> offers = rssJobProvider.fetchOffers(context);

        // THEN
        System.out.println("Fetched " + offers.size() + " offers from RSS feeds.");
        
        if (!offers.isEmpty()) {
            JobOfferRecord first = offers.get(0);
            System.out.println("Sample offer: " + first.title() + " at " + first.companyName());
            
            assertThat(first.title()).isNotBlank();
            assertThat(first.sourceUrl()).startsWith("http");
            assertThat(first.sourcePlatform()).isNotNull();
        }
    }
}
