package com.workdone.backend.joboffer.ingestion.remotive;

import com.workdone.backend.joboffer.ingestion.JobSearchParametersProvider;
import com.workdone.backend.joboffer.ingestion.SearchContext;
import com.workdone.backend.joboffer.analysis.*;
import com.workdone.backend.common.model.JobOfferRecord;
import com.workdone.backend.profile.parser.CvSemanticParser;
import com.workdone.backend.profile.service.CandidateProfileService;
import com.workdone.backend.profile.service.CvAggregationService;
import com.workdone.backend.joboffer.storage.OfferVectorStore;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RemotiveJobProviderTest {

    @Autowired
    private RemotiveJobProvider remotiveJobProvider;

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
    void shouldFetchOffersFromRemotiveApi() {
        // GIVEN
        SearchContext context = SearchContext.builder()
                .keywords(List.of("java", "spring"))
                .location("poland")
                .remoteOnly(true)
                .maxResults(50)
                .industry("engineering")
                .build();
                
        // WHEN
        List<JobOfferRecord> offers = remotiveJobProvider.fetchOffers(context);

        // THEN
        System.out.println("Fetched " + offers.size() + " offers from Remotive API.");
        
        if (!offers.isEmpty()) {
            JobOfferRecord first = offers.get(0);
            System.out.println("Sample Remotive offer: " + first.title() + " at " + first.companyName());
            
            assertThat(first.title()).isNotBlank();
            assertThat(first.sourceUrl()).startsWith("http");
            assertThat(first.sourcePlatform()).isEqualTo("REMOTIVE");
            assertThat(first.rawDescription()).isNotBlank();
        }
    }
}
