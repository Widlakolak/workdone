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
 * Serwis od powiadomień. Wysyła gotowe sformatowane wiadomości na moje kanały Discorda. 
 * Obsługuje alerty natychmiastowe, raporty dzienne i mój panel sterowania.
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
        if (!properties.discord().instant().enabled()) {
            log.debug("⏭️ Discord Instant disabled.");
            return;
        }
        String content = contentBuilder.buildInstantMessage(offer);
        post(properties.discord().instant().url(), instantPayload(content, offer.sourceUrl()));
    }

    public void sendDigest(List<JobOfferRecord> offers) {
        if (!properties.discord().digest().enabled()) {
            log.debug("⏭️ Discord Digest disabled.");
            return;
        }
        String content = contentBuilder.buildDigestMessage(offers);
        post(properties.discord().digest().url(), Map.of("content", content));
    }

    public void sendAiAlert(String message) {
        post(properties.discord().instant().url(), Map.of("content", "⚠️ **System Alert:** " + message));
    }

    public void sendControlPanel() {
        String url = properties.discord().instant().url();
        if (isBlank(url)) {
            log.error("❌ Nie można wysłać panelu: URL webhooka (workdone.discord.instant.url) jest pustY!");
            return;
        }

        log.info("📡 Próba wysłania panelu na URL: {}", url.substring(0, Math.min(url.length(), 30)) + "...");

        Map<String, Object> payload = Map.of(
                "content", "🎮 **WorkDone Master Control Panel**",
                "components", List.of(
                        Map.of(
                                "type", 1,
                                "components", List.of(
                                        Map.of("type", 2, "style", 1, "label", "📊 Status", "custom_id", "config|status"),
                                        Map.of("type", 2, "style", 1, "label", "⏳ Do decyzji", "custom_id", "config|pending"),
                                        Map.of("type", 2, "style", 3, "label", "🚀 Szukaj Ofert", "custom_id", "config|run_ingestion"),
                                        Map.of("type", 2, "style", 2, "label", "❓ Pomoc", "custom_id", "config|help")
                                )
                        ),
                        Map.of(
                                "type", 1,
                                "components", List.of(
                                        Map.of("type", 2, "style", 1, "label", "📄 Skille z CV", "custom_id", "config|use_cv_skills"),
                                        Map.of("type", 2, "style", 4, "label", "🔄 Odśwież CV", "custom_id", "config|refresh_cv")
                                )
                        ),
                        Map.of(
                                "type", 1,
                                "components", List.of(
                                        Map.of("type", 2, "style", 2, "label", "AI 70%", "custom_id", "config|semantic|0.7"),
                                        Map.of("type", 2, "style", 2, "label", "AI 80%", "custom_id", "config|semantic|0.8"),
                                        Map.of("type", 2, "style", 2, "label", "AI 90%", "custom_id", "config|semantic|0.9")
                                )
                        ),
                        Map.of(
                                "type", 1,
                                "components", List.of(
                                        Map.of("type", 2, "style", 2, "label", "⚡ Instant 0.7", "custom_id", "config|instant|0.7"),
                                        Map.of("type", 2, "style", 2, "label", "⚡ Instant 0.8", "custom_id", "config|instant|0.8"),
                                        Map.of("type", 2, "style", 2, "label", "⚡ Instant 0.9", "custom_id", "config|instant|0.9")
                                )
                        ),
                        Map.of(
                                "type", 1,
                                "components", List.of(
                                        Map.of("type", 2, "style", 2, "label", "📊 Digest 0.5", "custom_id", "config|digest|0.5"),
                                        Map.of("type", 2, "style", 2, "label", "📊 Digest 0.6", "custom_id", "config|digest|0.6"),
                                        Map.of("type", 2, "style", 2, "label", "📊 Digest 0.7", "custom_id", "config|digest|0.7")
                                )
                        )
                )
        );
        post(url, payload);
    }

    private Object instantPayload(String content, String sourceUrl) {
        return Map.of(
                "content", content,
                "components", List.of(
                        Map.of(
                                "type", 1,
                                "components", List.of(
                                        Map.of("type", 2, "style", 3, "label", "Aplikowano", "custom_id", "applied|" + sourceUrl),
                                        Map.of("type", 2, "style", 4, "label", "Odrzuć", "custom_id", "reject|" + sourceUrl)
                                )
                        )
                )
        );
    }

    private void post(String url, Object payload) {
        if (isBlank(url) || url.contains("dummy")) {
            log.warn("⚠️ Próba wysyłki na pusty lub testowy URL Discorda.");
            return;
        }
        try {
            client.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("✅ Wiadomość wysłana pomyślnie na Discorda.");
        } catch (Exception ex) {
            log.error("❌ Błąd podczas wysyłania do Discorda: {}", ex.getMessage());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
