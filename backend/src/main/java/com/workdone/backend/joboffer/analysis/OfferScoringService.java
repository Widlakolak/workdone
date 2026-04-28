package com.workdone.backend.joboffer.analysis;

import com.workdone.backend.common.config.WorkDoneProperties;
import com.workdone.backend.common.model.JobOfferRecord;
import com.workdone.backend.joboffer.notification.DiscordNotifier;
import com.workdone.backend.profile.service.CandidateProfileService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferScoringService {
    private static final Duration DEFAULT_RATE_LIMIT_COOLDOWN = Duration.ofSeconds(15);
    private static final Pattern RETRY_SECONDS_PATTERN = Pattern.compile("try again in\\s+([0-9]+(?:\\.[0-9]+)?)s", Pattern.CASE_INSENSITIVE);
    private static final Pattern RETRY_MILLIS_PATTERN = Pattern.compile("try again in\\s+([0-9]+(?:\\.[0-9]+)?)ms", Pattern.CASE_INSENSITIVE);

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
    private final ConcurrentMap<String, Instant> providerCooldownUntil = new ConcurrentHashMap<>();

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

        List<ModelAttempt> attempts = List.of(
                new ModelAttempt("Groq", groqClient),
                new ModelAttempt("OpenAI", openAiClient),
                new ModelAttempt("Gemini", geminiClient)
        );

        List<String> errors = new ArrayList<>();
        RuntimeException lastException = null;

        for (ModelAttempt attempt : attempts) {
            if (isProviderInCooldown(attempt.providerName())) {
                errors.add(attempt.providerName() + ": in cooldown after previous 429");
                continue;
            }
            try {
                return callAi(attempt.client(), userPrompt, attempt.providerName());
            } catch (Exception e) {
                lastException = (e instanceof RuntimeException runtimeException)
                        ? runtimeException
                        : new IllegalStateException("Provider call failed", e);
                String safe = safeMessage(e);
                errors.add(attempt.providerName() + ": " + safe);
                if (isRateLimitError(safe)) {
                    Duration cooldown = extractRetryDelay(safe);
                    putProviderOnCooldown(attempt.providerName(), cooldown);
                    log.warn("{} rate-limited ({}). Cooling down for {} ms.", attempt.providerName(), safe, cooldown.toMillis());
                } else {
                    log.warn("{} scoring failed, trying next provider. Cause: {}", attempt.providerName(), safe);
                }
            }
        }

        String summary = String.join(" | ", errors);
        log.error("All AI scoring models failed: {}", summary);
        discordNotifier.sendAiAlert("❌ CRITICAL: All AI scoring models failed! " + summary);
        throw (lastException != null ? lastException : new IllegalStateException("All providers unavailable"));
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

    private String safeMessage(Exception e) {
        return e == null ? "unknown" : Objects.toString(e.getMessage(), e.getClass().getSimpleName());
    }

    private boolean isRateLimitError(String message) {
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("429")
                || normalized.contains("rate limit")
                || normalized.contains("toomanyrequest")
                || normalized.contains("insufficient_quota");
    }

    private Duration extractRetryDelay(String message) {
        if (message == null || message.isBlank()) {
            return DEFAULT_RATE_LIMIT_COOLDOWN;
        }

        Matcher matcher = RETRY_TIME_PATTERN.matcher(message);
        if (matcher.find()) {
            long hours = matcher.group(1) != null ? Long.parseLong(matcher.group(1)) : 0;
            long minutes = matcher.group(2) != null ? Long.parseLong(matcher.group(2)) : 0;
            double seconds = matcher.group(3) != null ? Double.parseDouble(matcher.group(3)) : 0;

            // Przeliczamy wszystko na milisekundy
            long totalMillis = (long) ((hours * 3600 + minutes * 60 + seconds) * 1000);

            // Zwracamy wyliczony czas + 5 sekund marginesu bezpieczeństwa
            return Duration.ofMillis(Math.max(1000L, totalMillis + 5000L));
        }

        return DEFAULT_RATE_LIMIT_COOLDOWN;
    }

    private static final Pattern RETRY_TIME_PATTERN = Pattern.compile(
            "try again in (?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+(?:\\.\\d+)?)s)?",
            Pattern.CASE_INSENSITIVE
    );

    private void putProviderOnCooldown(String providerName, Duration cooldown) {
        providerCooldownUntil.put(providerName, Instant.now().plus(cooldown));
    }

    private boolean isProviderInCooldown(String providerName) {
        Instant blockedUntil = providerCooldownUntil.get(providerName);
        if (blockedUntil == null) {
            return false;
        }
        if (Instant.now().isAfter(blockedUntil)) {
            providerCooldownUntil.remove(providerName);
            return false;
        }
        return true;
    }

    private record ModelAttempt(String providerName, ChatClient client) {}

    private record LlmScoringPayload(int score, boolean mustHaveSatisfied, String reasoning) {}
}