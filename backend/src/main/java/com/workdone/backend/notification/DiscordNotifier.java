package com.workdone.backend.notification;

import com.workdone.backend.config.WorkDoneProperties;
import com.workdone.backend.format.OfferContentBuilder;
import com.workdone.backend.model.JobOfferRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import static org.apache.kafka.common.utils.Utils.safe;

import java.util.List;
import java.util.Map;

@Component
public class DiscordNotifier {

    private static final Logger log = LoggerFactory.getLogger(DiscordNotifier.class);

    private final RestClient client;
    private final WorkDoneProperties properties;
    private final OfferContentBuilder contentBuilder;

    public DiscordNotifier(WorkDoneProperties properties, OfferContentBuilder contentBuilder) {
        this.client = RestClient.create();
        this.properties = properties;
        this.contentBuilder = contentBuilder;
    }

    public void sendInstant(JobOfferRecord offer) {
        if (!properties.discord().instant().enabled() || isBlank(properties.discord().instant().url())) {
            return;
        }

        String content = contentBuilder.buildInstantMessage(offer);
        post(properties.discord().instant().url(), instantPayload(content, offer.sourceUrl()));
    }

    public void sendDigest(List<JobOfferRecord> offers) {
        if (offers.isEmpty() || !properties.discord().digest().enabled() || isBlank(properties.discord().digest().url())) {
            return;
        }

        String content = contentBuilder.buildDigestMessage(offers);
        post(properties.discord().digest().url(), Map.of("content", content));
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