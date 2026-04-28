package com.workdone.backend.joboffer.analysis;

import com.workdone.backend.joboffer.analysis.DynamicConfigService;
import com.workdone.backend.joboffer.analysis.MatchingBand;
import com.workdone.backend.joboffer.analysis.OfferClassificationService;
import com.workdone.backend.common.model.OfferStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class OfferClassificationServiceTest {

    private OfferClassificationService service;
    private DynamicConfigService dynamicConfig;

    @BeforeEach
    void setUp() {
        dynamicConfig = Mockito.mock(DynamicConfigService.class);
        // Konfigurujemy mocka, aby zwracał domyślne progi
        when(dynamicConfig.getInstantThreshold()).thenReturn(90.0);
        when(dynamicConfig.getDigestThreshold()).thenReturn(60.0);
        when(dynamicConfig.getArchiveThreshold()).thenReturn(30.0);
        
        service = new OfferClassificationService(dynamicConfig);
    }

    @Test
    void shouldClassifyBandsAndStatuses() {
        assertThat(service.classify(95.0)).isEqualTo(MatchingBand.INSTANT);
        assertThat(service.classify(70.0)).isEqualTo(MatchingBand.DIGEST);
        assertThat(service.classify(45.0)).isEqualTo(MatchingBand.TRACKING);
        assertThat(service.classify(20.0)).isEqualTo(MatchingBand.ARCHIVED);
        assertThat(service.toStatus(MatchingBand.ARCHIVED)).isEqualTo(OfferStatus.ARCHIVED);
    }
}