package com.workdone.backend.joboffer.orchestration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Pomiń test integracyjny całego potoku do czasu stabilizacji modeli AI")
class PipelineEndToEndTest {

    @Test
    void shouldProcessOfferFromIngestionToDiscord() {
    }
}
