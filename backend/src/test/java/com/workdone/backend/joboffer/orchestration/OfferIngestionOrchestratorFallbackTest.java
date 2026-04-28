package com.workdone.backend.joboffer.orchestration;

import com.workdone.backend.common.config.WorkDoneProperties;
import com.workdone.backend.joboffer.ingestion.JobProvider;
import com.workdone.backend.joboffer.ingestion.JobSearchParametersProvider;
import com.workdone.backend.joboffer.ingestion.SearchContext;
import com.workdone.backend.joboffer.analysis.*;
import com.workdone.backend.joboffer.orchestration.OfferEnricher;
import com.workdone.backend.joboffer.orchestration.OfferIngestionOrchestrator;
import com.workdone.backend.joboffer.orchestration.OfferProcessor;
import com.workdone.backend.common.model.JobOfferRecord;
import com.workdone.backend.common.model.OfferStatus;
import com.workdone.backend.joboffer.notification.DiscordNotifier;
import com.workdone.backend.profile.service.CandidateProfileService;
import com.workdone.backend.joboffer.storage.OfferStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OfferIngestionOrchestratorFallbackTest {

    private JobProvider provider;
    private OfferEnricher offerEnricher;
    private OfferStore store;
    private OfferProcessor offerProcessor;
    private CandidateProfileService candidateProfileService;
    private OfferClassificationService classificationService;
    private DiscordNotifier notifier;
    private WorkDoneProperties properties;
    private OfferEmbeddingService embeddingService;
    private JobSearchParametersProvider searchParametersProvider;
    private DynamicConfigService dynamicConfigService;
    private AiExecutionPolicy aiPolicy;
    private OfferIngestionOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        provider = mock(JobProvider.class);
        offerEnricher = mock(OfferEnricher.class);
        store = mock(OfferStore.class);
        offerProcessor = mock(OfferProcessor.class);
        candidateProfileService = mock(CandidateProfileService.class);
        classificationService = mock(OfferClassificationService.class);
        notifier = mock(DiscordNotifier.class);
        properties = mock(WorkDoneProperties.class);
        embeddingService = mock(OfferEmbeddingService.class);
        searchParametersProvider = mock(JobSearchParametersProvider.class);
        dynamicConfigService = mock(DynamicConfigService.class);
        aiPolicy = mock(AiExecutionPolicy.class);

        orchestrator = new OfferIngestionOrchestrator(
                List.of(provider),
                offerEnricher,
                store,
                offerProcessor,
                candidateProfileService,
                notifier,
                embeddingService,
                searchParametersProvider,
                dynamicConfigService,
                aiPolicy,
                classificationService
        );
    }

    @Test
    void shouldSendBestOfferWhenFallbackIsEnabledAndNoInstantOffers() {
        JobOfferRecord rawOffer = createSampleOffer("1");
        // Oferta po fazie 1 (semantyka)
        JobOfferRecord preProcessedOffer = rawOffer.withMatchingScore(80.0);
        // Oferta po fazie 2 (AI)
        JobOfferRecord finalOffer = preProcessedOffer.withPriorityScore(77.0).withStatus(OfferStatus.ANALYZED);

        SearchContext context = SearchContext.builder().location("Poland").build();

        when(candidateProfileService.getCandidateVector()).thenReturn(new float[]{0.1f});
        when(searchParametersProvider.getContexts()).thenReturn(List.of(context));
        when(provider.sourceName()).thenReturn("TEST_PROVIDER");
        when(provider.fetchOffers(context)).thenReturn(List.of(rawOffer));
        when(offerEnricher.cleanAndEnrich(rawOffer)).thenReturn(rawOffer);
        when(store.existsBySourceOrFingerprint(rawOffer)).thenReturn(false);
        when(embeddingService.embedOffers(any())).thenReturn(List.of(new float[]{0.1f}));

        // Mockujemy nową logikę OfferProcessor
        when(offerProcessor.preProcess(eq(rawOffer), any(), any()))
                .thenReturn(new OfferProcessor.ProcessingResult(preProcessedOffer, null, true, null, new float[]{0.1f}));

        when(offerProcessor.enrichWithAi(eq(preProcessedOffer), any(), any()))
                .thenReturn(new OfferProcessor.ProcessingResult(finalOffer, MatchingBand.DIGEST, true, OfferAnalysisFacade.AnalysisSource.AI, new float[]{0.1f}));
        when(dynamicConfigService.isBestOfferFallbackEnabled()).thenReturn(true);

        orchestrator.runIngestion();

        Mockito.verify(notifier).sendInstant(finalOffer);
    }

    @Test
    void shouldContinueIngestionWhenCandidateVectorIsMissing() {
        JobOfferRecord rawOffer = createSampleOffer("2");
        SearchContext context = SearchContext.builder().location("Poland").build();

        when(candidateProfileService.getCandidateVector()).thenReturn(null);
        when(searchParametersProvider.getContexts()).thenReturn(List.of(context));
        when(provider.sourceName()).thenReturn("TEST_PROVIDER");
        when(provider.fetchOffers(context)).thenReturn(List.of(rawOffer));
        when(offerEnricher.cleanAndEnrich(rawOffer)).thenReturn(rawOffer);
        when(store.existsBySourceOrFingerprint(rawOffer)).thenReturn(false);
        when(embeddingService.embedOffers(any())).thenReturn(List.of(new float[]{0.1f}));

        // Mockujemy pominięcie w fazie 1
        when(offerProcessor.preProcess(any(), any(), any())).thenReturn(OfferProcessor.ProcessingResult.skipped());

        orchestrator.runIngestion();

        Mockito.verify(provider).fetchOffers(context);
    }

    private JobOfferRecord createSampleOffer(String id) {
        return JobOfferRecord.builder()
                .id(id)
                .fingerprint("fp-" + id)
                .title("Offer " + id)
                .companyName("ACME")
                .sourceUrl("https://example.com/job/" + id)
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
    }
}