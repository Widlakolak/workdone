package com.workdone.backend.interaction.discord;

import com.workdone.backend.joboffer.orchestration.OfferIngestionOrchestrator;
import com.workdone.backend.profile.service.CandidateProfileService;
import com.workdone.backend.joboffer.analysis.DynamicConfigService;
import com.workdone.backend.common.model.JobOfferRecord;
import com.workdone.backend.common.model.OfferStatus;
import com.workdone.backend.joboffer.notification.DiscordNotifier;
import com.workdone.backend.joboffer.storage.InMemoryOfferStore;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiscordInteractionServiceTest {

    @Test
    void shouldUpdateOfferStatusWhenCustomIdIsValid() {
        InMemoryOfferStore store = new InMemoryOfferStore();
        DynamicConfigService dynamicConfig = Mockito.mock(DynamicConfigService.class);
        CandidateProfileService profileService = Mockito.mock(CandidateProfileService.class);
        OfferIngestionOrchestrator orchestrator = Mockito.mock(OfferIngestionOrchestrator.class);
        DiscordNotifier discordNotifier = Mockito.mock(DiscordNotifier.class);
        
        String targetUrl = "https://example.com/job/1";
        
        store.upsert(JobOfferRecord.builder()
                .id("id-1")
                .fingerprint("fp-1")
                .title("Java Developer")
                .companyName("ACME")
                .sourceUrl(targetUrl)
                .location("Łódź")
                .rawDescription("desc")
                .techStack(List.of("Java"))
                .matchingScore(95.0)
                .priorityScore(95.0)
                .status(OfferStatus.ANALYZED)
                .publishedAt(LocalDateTime.now())
                .sourcePlatform("JUST_JOIN_IT")
                .build());

        DiscordInteractionService service = new DiscordInteractionService(store, dynamicConfig, profileService, orchestrator, discordNotifier);
        String result = service.handleCustomId("applied|" + targetUrl);

        // Poprawiłem asercję, żeby pasowała do faktycznego komunikatu z serwisu
        assertThat(result).contains("✅ Zapisałem wybór: applied.");
        
        JobOfferRecord updated = store.findBySourceUrl(targetUrl).orElseThrow();
        assertThat(updated.status()).isEqualTo(OfferStatus.APPLIED);
    }

    @Test
    void shouldNotOverrideMustHaveWhenCvKeywordsAreEmpty() {
        InMemoryOfferStore store = new InMemoryOfferStore();
        DynamicConfigService dynamicConfig = Mockito.mock(DynamicConfigService.class);
        CandidateProfileService profileService = Mockito.mock(CandidateProfileService.class);
        OfferIngestionOrchestrator orchestrator = Mockito.mock(OfferIngestionOrchestrator.class);
        DiscordNotifier discordNotifier = Mockito.mock(DiscordNotifier.class);
        Mockito.when(profileService.getSuggestedKeywords()).thenReturn(List.of());

        DiscordInteractionService service = new DiscordInteractionService(store, dynamicConfig, profileService, orchestrator, discordNotifier);
        String result = service.handleCustomId("config|use_cv_skills");

        assertThat(result).contains("❌ Brak słów kluczowych z CV");
        Mockito.verify(dynamicConfig, Mockito.never()).updateConfig(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void shouldReturnErrorWhenCvRefreshFails() {
        InMemoryOfferStore store = new InMemoryOfferStore();
        DynamicConfigService dynamicConfig = Mockito.mock(DynamicConfigService.class);
        CandidateProfileService profileService = Mockito.mock(CandidateProfileService.class);
        OfferIngestionOrchestrator orchestrator = Mockito.mock(OfferIngestionOrchestrator.class);
        DiscordNotifier discordNotifier = Mockito.mock(DiscordNotifier.class);
        Mockito.when(profileService.refreshProfile()).thenReturn(false);

        DiscordInteractionService service = new DiscordInteractionService(store, dynamicConfig, profileService, orchestrator, discordNotifier);
        String result = service.handleCustomId("config|refresh_cv");

        assertThat(result).contains("❌ Nie udało się odświeżyć CV");
    }
}
