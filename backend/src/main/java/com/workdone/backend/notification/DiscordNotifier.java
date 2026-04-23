package com.workdone.backend.notification;

import com.workdone.backend.config.WorkDoneProperties;
import com.workdone.backend.format.OfferContentBuilder;
import com.workdone.backend.model.JobOfferRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * Serwis od powiadomień. 
 * Wykorzystuje Bot API dla interaktywnych paneli i Webhooki jako fallback.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordNotifier {

    private final WorkDoneProperties properties;
    private final OfferContentBuilder contentBuilder;

    private RestClient client;

    @PostConstruct
    public void init() {
        this.client = RestClient.create();
    }

    public void sendInstant(JobOfferRecord offer) {
        if (!properties.discord().instant().enabled()) return;
        String content = contentBuilder.buildInstantMessage(offer);
        postMessage(properties.discord().instant().url(), instantPayload(content, offer.sourceUrl()));
    }

    public void sendDigest(List<JobOfferRecord> offers) {
        if (!properties.discord().digest().enabled()) return;
        String content = contentBuilder.buildDigestMessage(offers);
        postMessage(properties.discord().digest().url(), Map.of("content", content));
    }

    public void sendAiAlert(String message) {
        postMessage(properties.discord().instant().url(), Map.of("content", "⚠️ **System Alert:** " + message));
    }

    public void sendControlPanel() {
        Map<String, Object> payload = Map.of(
                "content", "🎮 **WorkDone Master Control Panel**",
                "components", List.of(
                        Map.of("type", 1, "components", List.of(
                                Map.of("type", 2, "style", 1, "label", "📊 Status", "custom_id", "config|status"),
                                Map.of("type", 2, "style", 1, "label", "⏳ Do decyzji", "custom_id", "config|pending"),
                                Map.of("type", 2, "style", 3, "label", "🚀 Szukaj Ofert", "custom_id", "config|run_ingestion"),
                                Map.of("type", 2, "style", 2, "label", "❓ Pomoc", "custom_id", "config|help")
                        )),
                        Map.of("type", 1, "components", List.of(
                                Map.of("type", 2, "style", 1, "label", "📄 Skille z CV", "custom_id", "config|use_cv_skills"),
                                Map.of("type", 2, "style", 4, "label", "🔄 Odśwież CV", "custom_id", "config|refresh_cv")
                        )),
                        Map.of("type", 1, "components", List.of(
                                Map.of("type", 2, "style", 2, "label", "AI 70%", "custom_id", "config|semantic|0.7"),
                                Map.of("type", 2, "style", 2, "label", "AI 80%", "custom_id", "config|semantic|0.8"),
                                Map.of("type", 2, "style", 2, "label", "AI 90%", "custom_id", "config|semantic|0.9")
                        )),
                        Map.of("type", 1, "components", List.of(
                                Map.of("type", 2, "style", 2, "label", "⚡ Instant 0.7", "custom_id", "config|instant|0.7"),
                                Map.of("type", 2, "style", 2, "label", "⚡ Instant 0.8", "custom_id", "config|instant|0.8"),
                                Map.of("type", 2, "style", 2, "label", "⚡ Instant 0.9", "custom_id", "config|instant|0.9")
                        )),
                        Map.of("type", 1, "components", List.of(
                                Map.of("type", 2, "style", 2, "label", "📊 Digest 0.5", "custom_id", "config|digest|0.5"),
                                Map.of("type", 2, "style", 2, "label", "📊 Digest 0.6", "custom_id", "config|digest|0.6"),
                                Map.of("type", 2, "style", 2, "label", "📊 Digest 0.7", "custom_id", "config|digest|0.7")
                        ))
                )
        );
        postMessage(properties.discord().instant().url(), payload);
    }

    private void postMessage(String webhookUrl, Object payload) {
        String token = properties.discord().token();
        String channelId = properties.discord().channelId();
        
        if (!isBlank(token) && !isBlank(channelId)) {
            try {
                client.post()
                        .uri("https://discord.com/api/v10/channels/" + channelId + "/messages")
                        .header("Authorization", "Bot " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();
                log.info("✅ Wiadomość wysłana przez Bot API (Channel ID: {}).", channelId);
                return;
            } catch (Exception ex) {
                log.error("❌ Błąd Bot API (ID: {}): {}", channelId, ex.getMessage());
            }
        }
        
        // Fallback na Webhook
        if (!isBlank(webhookUrl)) {
            try {
                client.post()
                        .uri(webhookUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();
                log.info("✅ Wiadomość wysłana przez Webhook (fallback).");
            } catch (Exception ex) {
                log.error("❌ Błąd Webhooka: {}", ex.getMessage());
            }
        }
    }

    private Object instantPayload(String content, String sourceUrl) {
        return Map.of(
                "content", content,
                "components", List.of(
                        Map.of("type", 1, "components", List.of(
                                Map.of("type", 2, "style", 3, "label", "Aplikowano", "custom_id", "applied|" + sourceUrl),
                                Map.of("type", 2, "style", 4, "label", "Odrzuć", "custom_id", "reject|" + sourceUrl)
                        ))
                )
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
