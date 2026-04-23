package com.workdone.backend.ingestion.jobicy;

import com.workdone.backend.analysis.*;
import com.workdone.backend.ingestion.JobSearchParametersProvider;
import com.workdone.backend.ingestion.SearchContext;
import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.profile.parser.CvSemanticParser;
import com.workdone.backend.profile.service.CandidateProfileService;
import com.workdone.backend.profile.service.CvAggregationService;
import com.workdone.backend.storage.OfferVectorStore;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class JobicyJobProviderTest {

    @Autowired
    private JobicyJobProvider jobicyJobProvider;

    @MockitoBean private JobSearchParametersProvider jobSearchParametersProvider;

    // Mockujemy zależności AI i inne, aby test nie wymagał kluczy API i zewnętrznych serwisów
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
    void shouldFetchOffersFromJobicyApi() {
        // GIVEN
        SearchContext context = SearchContext.builder()
                .keywords(List.of("java", "spring"))
                .location("poland")
                .remoteOnly(true)
                .maxResults(50)
                .industry("engineering")
                .build();
                
        // WHEN
        List<JobOfferRecord> offers = jobicyJobProvider.fetchOffers(context);

        // THEN
        System.out.println("Fetched " + offers.size() + " offers from Jobicy API.");
        
        if (!offers.isEmpty()) {
            JobOfferRecord first = offers.get(0);
            System.out.println("Sample Jobicy offer: " + first.title() + " at " + first.companyName());
            
            assertThat(first.title()).isNotBlank();
            assertThat(first.sourceUrl()).startsWith("http");
            assertThat(first.sourcePlatform()).isEqualTo("JOBICY");
            assertThat(first.rawDescription()).isNotBlank();
        }
    }
}
