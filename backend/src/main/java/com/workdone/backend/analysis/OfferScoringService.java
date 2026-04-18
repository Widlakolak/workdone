package com.workdone.backend.analysis;

import com.workdone.backend.config.WorkDoneProperties;
import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.profile.service.CvAggregationService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class OfferScoringService {

    private static final String SYSTEM_PROMPT = """
            You are an expert technical recruiter.
            Evaluate a job offer for one candidate profile.
            Return strict JSON with fields:
            - score: number from 0 to 100
            - mustHaveSatisfied: boolean
            - reasoning: short explanation (max 300 chars)
            """;

    private final ChatClient chatClient;
    private final WorkDoneProperties properties;
    private final CvAggregationService cvAggregationService;

    private volatile String cachedProfile;

    public OfferScoringService(ChatClient.Builder chatClientBuilder,
                               WorkDoneProperties properties,
                               CvAggregationService cvAggregationService) {
        this.chatClient = chatClientBuilder.build();
        this.properties = properties;
        this.cvAggregationService = cvAggregationService;
    }

    private String getProfile() {
        if (cachedProfile == null) {
            synchronized (this) {
                if (cachedProfile == null) {
                    cachedProfile = cvAggregationService.buildMergedProfile();
                }
            }
        }
        return cachedProfile;
    }

    public OfferScoringResult score(JobOfferRecord offer) {

        String profile = getProfile();

        LlmScoringPayload response = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(buildUserPrompt(offer, profile))
                .call()
                .entity(LlmScoringPayload.class);

        if (response == null) {
            throw new IllegalStateException("Model returned empty scoring payload");
        }

        double normalizedScore = clamp(response.score(), 0.0, 100.0);
        String reasoning = response.reasoning() == null ? "" : response.reasoning().trim();

        return new OfferScoringResult(
                normalizedScore,
                response.mustHaveSatisfied(),
                reasoning
        );
    }

    private String buildUserPrompt(JobOfferRecord offer, String profile) {
        List<String> mustHave = properties.matching().mustHaveKeywords();
        String mustHaveLine = (mustHave == null || mustHave.isEmpty())
                ? "none"
                : String.join(", ", mustHave);

        return """
                Candidate profile:
                %s

                Must-have keywords:
                %s

                Offer title: %s
                Company: %s
                Location: %s
                Tech stack: %s
                Description:
                %s
                """.formatted(
                profile,
                mustHaveLine,
                defaultText(offer.title()),
                defaultText(offer.companyName()),
                defaultText(offer.location()),
                Objects.toString(offer.techStack(), "[]"),
                defaultText(offer.rawDescription())
        );
    }

    private static String defaultText(String value) {
        return value == null ? "" : value;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record LlmScoringPayload(
            int score,
            boolean mustHaveSatisfied,
            String reasoning
    ) {}
}