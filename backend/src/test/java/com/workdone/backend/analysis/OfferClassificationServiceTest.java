package com.workdone.backend.analysis;

import com.workdone.backend.config.WorkDoneProperties;
import com.workdone.backend.model.OfferStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OfferClassificationServiceTest {

    private final OfferClassificationService service = new OfferClassificationService(
            new WorkDoneProperties(
                    new WorkDoneProperties.Profile("/tmp"),
                    new WorkDoneProperties.Matching(90.0, 60.0, 40.0, List.of("java"), 50, 85),
                    new WorkDoneProperties.Scheduling("0 0 */2 * * *", "0 0 18 * * *", "Europe/Warsaw"),
                    new WorkDoneProperties.Discord(
                            new WorkDoneProperties.Webhook(false, ""),
                            new WorkDoneProperties.Webhook(false, "")
                    ),
                    null
            )
    );

    @Test
    void shouldClassifyBandsAndStatuses() {
        assertThat(service.classify(95.0)).isEqualTo(MatchingBand.INSTANT);
        assertThat(service.classify(70.0)).isEqualTo(MatchingBand.DIGEST);
        assertThat(service.classify(45.0)).isEqualTo(MatchingBand.TRACK_ONLY);
        assertThat(service.classify(20.0)).isEqualTo(MatchingBand.ARCHIVE);
        assertThat(service.toStatus(MatchingBand.ARCHIVE)).isEqualTo(OfferStatus.ARCHIVED);
    }
}