package com.workdone.backend.joboffer.analysis;

import com.workdone.backend.joboffer.analysis.DynamicConfigService;
import com.workdone.backend.joboffer.analysis.MustHaveGroupConfig;
import com.workdone.backend.joboffer.analysis.OfferMatchingService;
import com.workdone.backend.common.model.JobOfferRecord;
import com.workdone.backend.common.model.OfferStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OfferMatchingServiceTest {

    @Test
    void shouldMatchKeywordsWithSpacesAndHyphensAfterNormalization() {
        MustHaveGroupConfig groupConfig = Mockito.mock(MustHaveGroupConfig.class);
        DynamicConfigService dynamicConfig = Mockito.mock(DynamicConfigService.class);
        Mockito.when(groupConfig.groups()).thenReturn(List.of());
        Mockito.when(groupConfig.minGroupsToPass()).thenReturn(0);
        Mockito.when(dynamicConfig.getMustHaveKeywords()).thenReturn(List.of("spring boot", "ci-cd"));

        OfferMatchingService service = new OfferMatchingService(groupConfig, dynamicConfig);
        JobOfferRecord offer = JobOfferRecord.builder()
                .id("1")
                .fingerprint("fp")
                .title("Java Developer")
                .companyName("ACME")
                .sourceUrl("https://example.com/job/1")
                .location("Łódź")
                .rawDescription("Szukamy osoby z doświadczeniem w Spring Boot oraz CI CD")
                .salaryRange("n/a")
                .techStack(List.of("Java", "Spring Boot"))
                .matchingScore(0.0)
                .priorityScore(0.0)
                .status(OfferStatus.NEW)
                .publishedAt(LocalDateTime.now())
                .sourcePlatform("TEST")
                .build();

        assertThat(service.passesMustHave(offer)).isTrue();
    }

    @Test
    void shouldPassCoreMustHaveWhenRequiredGroupsMatch() {
        MustHaveGroupConfig groupConfig = Mockito.mock(MustHaveGroupConfig.class);
        DynamicConfigService dynamicConfig = Mockito.mock(DynamicConfigService.class);
        Mockito.when(groupConfig.groups()).thenReturn(List.of(
                new MustHaveGroup("language", List.of("java"), true),
                new MustHaveGroup("framework", List.of("spring boot"), true),
                new MustHaveGroup("database", List.of("sql"), false)
        ));
        Mockito.when(dynamicConfig.getMustHaveKeywords()).thenReturn(List.of());

        OfferMatchingService service = new OfferMatchingService(groupConfig, dynamicConfig);
        JobOfferRecord offer = JobOfferRecord.builder()
                .id("2")
                .fingerprint("fp2")
                .title("Java Backend Engineer")
                .companyName("ACME")
                .sourceUrl("https://example.com/job/2")
                .location("Remote")
                .rawDescription("Projekt w Spring Boot dla backendu")
                .salaryRange("n/a")
                .techStack(List.of("Java", "Spring Boot"))
                .matchingScore(0.0)
                .priorityScore(0.0)
                .status(OfferStatus.NEW)
                .publishedAt(LocalDateTime.now())
                .sourcePlatform("TEST")
                .build();

        assertThat(service.passesCoreMustHave(offer)).isTrue();
    }
}