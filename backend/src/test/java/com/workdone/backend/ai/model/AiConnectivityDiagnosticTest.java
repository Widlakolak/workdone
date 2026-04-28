package com.workdone.backend.ai.model;

import com.workdone.backend.joboffer.analysis.OfferEmbeddingService;
import com.workdone.backend.joboffer.analysis.OfferScoringResult;
import com.workdone.backend.joboffer.analysis.OfferScoringService;
import com.workdone.backend.common.model.JobOfferRecord;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Uruchamiaj tylko ręcznie po wklejeniu kluczy do application-test.yaml")
class AiConnectivityDiagnosticTest {

    @Autowired
    private OfferScoringService scoringService;

    @Autowired
    private OfferEmbeddingService embeddingService;

    @Test
    void diagnosticCheckAllAiApis() {
        JobOfferRecord testOffer = JobOfferRecord.builder()
                .title("Java Developer")
                .companyName("Diagnostic")
                .sourceUrl("http://diag.com")
                .rawDescription("We need Java and Spring Boot expert.")
                .build();

        System.out.println("\n--- [AI DIAGNOSTIC START] ---");

        try {
            float[] vector = embeddingService.embed("Hello world");
            System.out.println("✅ Embedding API: OK (Vector size: " + vector.length + ")");
        } catch (Exception e) {
            System.err.println("❌ Embedding API: FAILED - " + e.getMessage());
        }

        try {
            OfferScoringResult result = scoringService.score(testOffer);
            System.out.println("✅ Scoring Pipeline: OK");
            System.out.println("   Final Score: " + result.score());
            System.out.println("   Reasoning: " + result.reasoning());
        } catch (Exception e) {
            System.err.println("❌ Scoring Pipeline: TOTAL FAILURE - " + e.getMessage());
        }

        System.out.println("--- [AI DIAGNOSTIC END] ---\n");
    }
}
