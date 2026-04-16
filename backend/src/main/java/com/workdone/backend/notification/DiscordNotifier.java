package com.workdone.backend.notification;

import com.workdone.backend.config.WorkDoneProperties;
import com.workdone.backend.model.JobOfferRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class DiscordNotifier {

    private static final Logger log = LoggerFactory.getLogger(DiscordNotifier.class);

    private final RestClient client;
    private final WorkDoneProperties properties;

    public DiscordNotifier(WorkDoneProperties properties) {
        this.client = RestClient.create();
        this.properties = properties;
    }

    public void sendInstant(JobOfferRecord offer) {
        if (!properties.discord().instant().enabled() || isBlank(properties.discord().instant().url())) {
            return;
        }

        String content = "🔥 **Instant Match >=90%**\n"
                + offer.title() + " @ " + offer.companyName() + "\n"
                + "Score: " + offer.matchingScore() + "%\n"
                + offer.sourceUrl();

        post(properties.discord().instant().url(), instantPayload(content, offer.sourceUrl()));
    }

    public void sendDigest(List<JobOfferRecord> offers) {
        if (offers.isEmpty() || !properties.discord().digest().enabled() || isBlank(properties.discord().digest().url())) {
            return;
        }

        StringBuilder message = new StringBuilder("📋 **Daily Digest (>=60%)**\n");
        offers.stream()
                .limit(20)
                .forEach(offer -> message
                        .append("- ")
                        .append(offer.title())
                        .append(" @ ")
                        .append(offer.companyName())
                        .append(" [")
                        .append(offer.matchingScore())
                        .append("%]\n")
                        .append(offer.sourceUrl())
                        .append("\n"));

        post(properties.discord().digest().url(), Map.of("content", message.toString()));
    }

    private Object instantPayload(String content, String sourceUrl) {
        return Map.of(
                "content", content,
                "components", List.of(
                        Map.of(
                                "type", 1,
                                "components", List.of(
                                        Map.of(
                                                "type", 2,
                                                "style", 3,
                                                "label", "Applied",
                                                "custom_id", "applied|" + sourceUrl
                                        ),
                                        Map.of(
                                                "type", 2,
                                                "style", 4,
                                                "label", "Reject",
                                                "custom_id", "reject|" + sourceUrl
                                        )
                                )
                        )
                )
        );
    }

    private void post(String url, Object payload) {
        try {
            client.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Nie udało się wysłać wiadomości na Discord", ex);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}