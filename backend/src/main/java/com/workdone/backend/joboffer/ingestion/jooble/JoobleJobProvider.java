package com.workdone.backend.joboffer.ingestion.jooble;

import com.workdone.backend.joboffer.analysis.LocationGuard;
import com.workdone.backend.common.util.ProviderCallExecutor;
import com.workdone.backend.joboffer.ingestion.JobProvider;
import com.workdone.backend.joboffer.ingestion.SearchContext;
import com.workdone.backend.common.model.JobOfferRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@EnableConfigurationProperties(JoobleProperties.class)
@ConditionalOnProperty(prefix = "workdone.providers.jooble", name = "enabled", havingValue = "true")
public class JoobleJobProvider implements JobProvider {

    private static final String SOURCE_NAME = "JOOBLE";

    private final RestClient restClient;
    private final JoobleProperties properties;
    private final JoobleMapper mapper;
    private final LocationGuard locationGuard;
    private final ProviderCallExecutor callExecutor;

    public JoobleJobProvider(@Qualifier("workDoneRestClientBuilder") RestClient.Builder restClientBuilder,
                             JoobleProperties properties,
                             JoobleMapper mapper,
                             LocationGuard locationGuard,
                             ProviderCallExecutor callExecutor) {
        String baseUrl = properties.resolvedUrl();
        this.restClient = restClientBuilder
                .baseUrl(baseUrl != null ? baseUrl : "")
                .build();
        this.properties = properties;
        this.mapper = mapper;
        this.locationGuard = locationGuard;
        this.callExecutor = callExecutor;
    }

    @PostConstruct
    void validateConfiguration() {
        if (!properties.enabled()) {
            return;
        }
        if (properties.resolvedUrl() == null) {
            throw new IllegalStateException("Jooble provider is enabled, but URL is missing. Configure workdone.providers.jooble.url or workdone.providers.jooble.base-url");
        }
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException("Jooble provider is enabled, but API key is missing. Configure workdone.providers.jooble.api-key");
        }
    }

    @Override
    public String sourceName() {
        return SOURCE_NAME;
    }

    @Override
    public List<JobOfferRecord> fetchOffers(SearchContext context) {
        String apiKey = properties.apiKey();
        String baseUrl = properties.resolvedUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            log.error("❌ Jooble fetch skipped: provider URL is missing (workdone.providers.jooble.url / base-url)");
            return Collections.emptyList();
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.error("❌ Jooble fetch skipped: API key is missing (workdone.providers.jooble.api-key)");
            return Collections.emptyList();
        }

        String fullUrl = baseUrl.endsWith("/") ? baseUrl + apiKey : baseUrl + "/" + apiKey;
        String location = normalizeLocation(context.location());

        try {
            JoobleResponse response = callExecutor.execute(SOURCE_NAME, () -> restClient.post()
                    .uri(java.net.URI.create(fullUrl))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "keywords", String.join(" ", context.keywords()),
                            "location", location
                    ))
                    .retrieve()
                    .body(JoobleResponse.class));

            if (response == null || response.jobs() == null || response.jobs().isEmpty()) {
                return Collections.emptyList();
            }

            return response.jobs().stream()
                    .map(mapper::toDomain)
                    .filter(locationGuard::isAccepted)
                    .toList();

        } catch (Exception e) {
            log.error("❌ Jooble Fetch Error: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String normalizeLocation(String location) {
        if (location == null || SearchContext.REMOTE_GLOBAL.equalsIgnoreCase(location)) {
            return "";
        }
        return location;
    }
}