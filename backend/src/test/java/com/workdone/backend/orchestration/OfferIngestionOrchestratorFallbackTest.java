package com.workdone.backend.orchestration;

import com.workdone.backend.analysis.DynamicConfigService;
import com.workdone.backend.analysis.MatchingBand;
import com.workdone.backend.analysis.OfferClassificationService;
import com.workdone.backend.analysis.OfferEmbeddingService;
import com.workdone.backend.config.WorkDoneProperties;
import com.workdone.backend.ingestion.JobProvider;
import com.workdone.backend.ingestion.JobSearchParametersProvider;
import com.workdone.backend.ingestion.SearchContext;
import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.model.OfferStatus;
import com.workdone.backend.notification.DiscordNotifier;
import com.workdone.backend.profile.service.CandidateProfileService;
import com.workdone.backend.storage.OfferStore;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

class OfferIngestionOrchestratorFallbackTest {

    @Test
    void shouldSendBestOfferWhenFallbackIsEnabledAndNoInstantOffers() {
        JobProvider provider = Mockito.mock(JobProvider.class);
        OfferEnricher offerEnricher = Mockito.mock(OfferEnricher.class);
        OfferStore store = Mockito.mock(OfferStore.class);
        OfferProcessor offerProcessor = Mockito.mock(OfferProcessor.class);
        CandidateProfileService candidateProfileService = Mockito.mock(CandidateProfileService.class);
        OfferClassificationService classificationService = Mockito.mock(OfferClassificationService.class);
        DiscordNotifier notifier = Mockito.mock(DiscordNotifier.class);
        WorkDoneProperties properties = Mockito.mock(WorkDoneProperties.class);
        OfferEmbeddingService embeddingService = Mockito.mock(OfferEmbeddingService.class);
        JobSearchParametersProvider searchParametersProvider = Mockito.mock(JobSearchParametersProvider.class);
        DynamicConfigService dynamicConfigService = Mockito.mock(DynamicConfigService.class);

        OfferIngestionOrchestrator orchestrator = new OfferIngestionOrchestrator(
                List.of(provider),
                offerEnricher,
                store,
                offerProcessor,
                candidateProfileService,
                classificationService,
                notifier,
                properties,
                embeddingService,
                searchParametersProvider,
                dynamicConfigService
        );

        JobOfferRecord rawOffer = JobOfferRecord.builder()
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

        JobOfferRecord analyzedOffer = rawOffer.withPriorityScore(77.0).withStatus(OfferStatus.ANALYZED);
        SearchContext context = SearchContext.builder().keywords(List.of("java")).location("Poland").remoteOnly(true).maxResults(10).build();

        Mockito.when(candidateProfileService.getCandidateVector()).thenReturn(new float[] {0.1f});
        Mockito.when(searchParametersProvider.getContexts()).thenReturn(List.of(context));
        Mockito.when(provider.sourceName()).thenReturn("TEST_PROVIDER");
        Mockito.when(provider.fetchOffers(context)).thenReturn(List.of(rawOffer));
        Mockito.when(offerEnricher.cleanAndEnrich(rawOffer)).thenReturn(rawOffer);
        Mockito.when(store.existsBySourceOrFingerprint(rawOffer)).thenReturn(false);
        Mockito.when(embeddingService.embedOffers(List.of(rawOffer))).thenReturn(List.of(new float[] {0.1f}));
        Mockito.when(offerProcessor.processOffer(Mockito.eq(rawOffer), Mockito.any(float[].class), Mockito.any(float[].class)))
                .thenReturn(new OfferProcessor.ProcessingResult(analyzedOffer, MatchingBand.DIGEST, true));
        Mockito.when(dynamicConfigService.isBestOfferFallbackEnabled()).thenReturn(true);

        orchestrator.runIngestion();

        Mockito.verify(notifier).sendInstant(analyzedOffer);
    }


    @Test
    void shouldContinueIngestionWhenCandidateVectorIsMissing() {
        JobProvider provider = Mockito.mock(JobProvider.class);
        OfferEnricher offerEnricher = Mockito.mock(OfferEnricher.class);
        OfferStore store = Mockito.mock(OfferStore.class);
        OfferProcessor offerProcessor = Mockito.mock(OfferProcessor.class);
        CandidateProfileService candidateProfileService = Mockito.mock(CandidateProfileService.class);
        OfferClassificationService classificationService = Mockito.mock(OfferClassificationService.class);
        DiscordNotifier notifier = Mockito.mock(DiscordNotifier.class);
        WorkDoneProperties properties = Mockito.mock(WorkDoneProperties.class);
        OfferEmbeddingService embeddingService = Mockito.mock(OfferEmbeddingService.class);
        JobSearchParametersProvider searchParametersProvider = Mockito.mock(JobSearchParametersProvider.class);
        DynamicConfigService dynamicConfigService = Mockito.mock(DynamicConfigService.class);

        OfferIngestionOrchestrator orchestrator = new OfferIngestionOrchestrator(
                List.of(provider),
                offerEnricher,
                store,
                offerProcessor,
                candidateProfileService,
                classificationService,
                notifier,
                properties,
                embeddingService,
                searchParametersProvider,
                dynamicConfigService
        );

        JobOfferRecord rawOffer = JobOfferRecord.builder()
                .id("2")
                .fingerprint("fp-2")
                .title("Backend Developer")
                .companyName("ACME")
                .sourceUrl("https://example.com/job/2")
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

        SearchContext context = SearchContext.builder().keywords(List.of("java")).location("Poland").remoteOnly(true).maxResults(10).build();

        Mockito.when(candidateProfileService.getCandidateVector()).thenReturn(null);
        Mockito.when(searchParametersProvider.getContexts()).thenReturn(List.of(context));
        Mockito.when(provider.sourceName()).thenReturn("TEST_PROVIDER");
        Mockito.when(provider.fetchOffers(context)).thenReturn(List.of(rawOffer));
        Mockito.when(offerEnricher.cleanAndEnrich(rawOffer)).thenReturn(rawOffer);
        Mockito.when(store.existsBySourceOrFingerprint(rawOffer)).thenReturn(false);
        Mockito.when(embeddingService.embedOffers(List.of(rawOffer))).thenReturn(List.of(new float[] {0.1f}));
        Mockito.when(offerProcessor.processOffer(Mockito.eq(rawOffer), Mockito.any(float[].class), Mockito.any(float[].class)))
                .thenReturn(OfferProcessor.ProcessingResult.skipped());
        Mockito.when(dynamicConfigService.isBestOfferFallbackEnabled()).thenReturn(false);

        orchestrator.runIngestion();

        Mockito.verify(provider).fetchOffers(context);
        Mockito.verify(offerProcessor).processOffer(Mockito.eq(rawOffer), Mockito.any(float[].class), Mockito.any(float[].class));
    }
}