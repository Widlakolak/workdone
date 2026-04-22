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
        // Tworzę klient HTTP do strzelania w webhooki Discorda
        this.client = RestClient.create();
    }

    /**
     * Jak wpadnie super oferta (INSTANT), to od razu ją tu wysyłam z przyciskami do akcji.
     */
    public void sendInstant(JobOfferRecord offer) {
        if (!properties.discord().instant().enabled() || isBlank(properties.discord().instant().url())) {
            log.debug("⏭️ Discord Instant wyłączony albo brak URL-a, pomijam.");
            return;
        }

        log.info("🚀 Lecę z alertem Instant na Discorda: {}", offer.title());
        String content = contentBuilder.buildInstantMessage(offer);
        post(properties.discord().instant().url(), instantPayload(content, offer.sourceUrl()));
    }

    /**
     * Raz dziennie wysyłam listę "całkiem niezłych" ofert, które nie są krytyczne.
     */
    public void sendDigest(List<JobOfferRecord> offers) {
        if (offers.isEmpty() || !properties.discord().digest().enabled() || isBlank(properties.discord().digest().url())) {
            log.debug("⏭️ Nie wysyłam Digest (brak ofert albo wyłączone).");
            return;
        }

        log.info("📊 Wysyłam raport dzienny na Discorda (ofert: {})", offers.size());
        String content = contentBuilder.buildDigestMessage(offers);
        post(properties.discord().digest().url(), Map.of("content", content));
    }

    /**
     * Wysyła ogólny alert o błędach AI lub innych problemach systemowych.
     */
    public void sendAiAlert(String message) {
        if (isBlank(properties.discord().instant().url())) {
            log.debug("⏭️ Brak URL-a, nie mogę wysłać alertu AI.");
            return;
        }
        log.warn("📢 Wysyłam alert systemowy na Discorda: {}", message);
        post(properties.discord().instant().url(), Map.of("content", "⚠️ **System Alert:** " + message));
    }

    /**
     * Wysyła wiadomość z przyciskami, którymi mogę sterować botem z poziomu Discorda.
     */
    public void sendControlPanel() {
        if (isBlank(properties.discord().instant().url())) {
            log.debug("⏭️ Brak URL-a, nie mogę wysłać panelu kontrolnego.");
            return;
        }

        Map<String, Object> payload = Map.of(
                "content", "🛠 **WorkDone Control Panel**",
                "components", List.of(
                        Map.of(
                                "type", 1, // Row
                                "components", List.of(
                                        Map.of("type", 2, "style", 1, "label", "📊 Status", "custom_id", "config|status"),
                                        Map.of("type", 2, "style", 3, "label", "🚀 Uruchom Ingestion", "custom_id", "config|run_ingestion"),
                                        Map.of("type", 2, "style", 1, "label", "📄 Skille z CV", "custom_id", "config|use_cv_skills")
                                )
                        ),
                        Map.of(
                                "type", 1, // Row 2
                                "components", List.of(
                                        Map.of("type", 2, "style", 2, "label", "Semantic 80%", "custom_id", "config|semantic|80"),
                                        Map.of("type", 2, "style", 2, "label", "Semantic 70%", "custom_id", "config|semantic|70"),
                                        Map.of("type", 2, "style", 4, "label", "🔄 Odśwież CV", "custom_id", "config|refresh_cv")
                                )
                        )
                )
        );
        post(properties.discord().instant().url(), payload);
    }

    private Object instantPayload(String content, String sourceUrl) {
        // Składam JSON-a z treścią wiadomości i interaktywnymi przyciskami (Applied/Reject)
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
        if (url == null || url.isBlank() || url.contains("dummy")) {
            return;
        }

        try {
            client.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("❌ Nie udało się dobić do Discorda", ex);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
