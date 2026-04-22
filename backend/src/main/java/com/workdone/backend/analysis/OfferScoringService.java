package com.workdone.backend.analysis;

import com.workdone.backend.config.WorkDoneProperties;
import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.notification.DiscordNotifier;
import com.workdone.backend.profile.service.CandidateProfileService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferScoringService {

    private static final String SYSTEM_PROMPT = """
            You are an expert technical recruiter.
            Evaluate a job offer for one candidate profile.
            Return strict JSON with fields:
            - score: number from 0 to 100
            - mustHaveSatisfied: boolean
            - reasoning: short explanation (max 300 chars)
            """;

    @Qualifier("groqChatModel")
    private final ChatModel groqModel;
    @Qualifier("openAiChatModel")
    private final ChatModel openAiModel;
    @Qualifier("googleGenAiChatModel")
    private final ChatModel geminiModel;

    private final WorkDoneProperties properties;
    private final DynamicConfigService dynamicConfig;
    private final CandidateProfileService profileService;
    private final DiscordNotifier discordNotifier; // Wstrzykujemy DiscordNotifier

    private ChatClient groqClient;
    private ChatClient openAiClient;
    private ChatClient geminiClient;

    @PostConstruct
    public void init() {
        // Przygotowuję klientów dla każdego modelu AI z tym samym instruktażem systemowym
        this.groqClient = ChatClient.builder(groqModel).defaultSystem(SYSTEM_PROMPT).build();
        this.openAiClient = ChatClient.builder(openAiModel).defaultSystem(SYSTEM_PROMPT).build();
        this.geminiClient = ChatClient.builder(geminiModel).defaultSystem(SYSTEM_PROMPT).build();
    }

    public OfferScoringResult score(JobOfferRecord offer) {
        String profileText = profileService.getLatestProfileText();
        String userPrompt = buildUserPrompt(offer, profileText);

        // Mechanizm fallbacków: najpierw lecę najtańszym/najszybszym (Groq), 
        // a jak sypnie błędami, to idę w droższe/stabilniejsze (OpenAI, na końcu Gemini)
        try {
            return callAi(groqClient, userPrompt, "Groq");
        } catch (Exception e) {
            log.warn("Groq scoring failed, falling back to OpenAI...");
            discordNotifier.sendAiAlert("Groq failed (" + e.getMessage() + "), falling back to OpenAI...");
            try {
                return callAi(openAiClient, userPrompt, "OpenAI");
            } catch (Exception ex) {
                log.warn("OpenAI scoring failed, falling back to Gemini...");
                discordNotifier.sendAiAlert("OpenAI failed (" + ex.getMessage() + "), falling back to Gemini...");
                try {
                    return callAi(geminiClient, userPrompt, "Gemini");
                } catch (Exception efinal) {
                    log.error("All AI scoring models failed.");
                    discordNotifier.sendAiAlert("❌ CRITICAL: All AI scoring models failed! Deep analysis is currently unavailable.");
                    throw efinal;
                }
            }
        }
    }

    private OfferScoringResult callAi(ChatClient client, String userPrompt, String modelName) {
        log.info("Requesting score via {}...", modelName);
        LlmScoringPayload response = client.prompt()
                .user(userPrompt)
                .call()
                .entity(LlmScoringPayload.class);

        if (response == null) throw new IllegalStateException(modelName + " returned empty payload");

        return new OfferScoringResult(
                clamp(response.score(), 0.0, 100.0),
                response.mustHaveSatisfied(),
                response.reasoning() != null ? response.reasoning().trim() : ""
        );
    }

    private String buildUserPrompt(JobOfferRecord offer, String profile) {
        // Składam prompta tak, żeby AI wiedziało co jest dla mnie priorytetem (słowa kluczowe z bota)
        List<String> mustHave = dynamicConfig.getMustHaveKeywords();

        return """
                Candidate profile (latest CV):
                %s
                
                Current MUST-HAVE Keywords (Candidate's Priority): 
                %s
                
                Offer: %s at %s (%s)
                Tech stack: %s
                Description: %s
                """.formatted(
                profile,
                (mustHave == null || mustHave.isEmpty()) ? "none" : String.join(", ", mustHave),
                offer.title(), offer.companyName(), offer.location(),
                Objects.toString(offer.techStack(), "[]"),
                offer.rawDescription()
        );
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record LlmScoringPayload(int score, boolean mustHaveSatisfied, String reasoning) {}
}
