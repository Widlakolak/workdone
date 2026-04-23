package com.workdone.backend.ingestion.remotive;

import com.workdone.backend.analysis.LocationGuard;
import com.workdone.backend.ingestion.JobProvider;
import com.workdone.backend.ingestion.SearchContext;
import com.workdone.backend.model.JobOfferRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@EnableConfigurationProperties(RemotiveProperties.class)
@ConditionalOnProperty(prefix = "workdone.providers.remotive", name = "enabled", havingValue = "true")
public class RemotiveJobProvider implements JobProvider {

    private static final String SOURCE_NAME = "REMOTIVE";

    private final RestClient restClient;
    private final RemotiveProperties properties;
    private final RemotiveMapper mapper;
    private final LocationGuard locationGuard;

    public RemotiveJobProvider(@Qualifier("workDoneRestClientBuilder") RestClient.Builder restClientBuilder,
                               RemotiveProperties properties,
                               RemotiveMapper mapper,
                               LocationGuard locationGuard) {
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .build();
        this.properties = properties;
        this.mapper = mapper;
        this.locationGuard = locationGuard;
    }

    @Override
    public String sourceName() {
        return SOURCE_NAME;
    }

    @Override
    public List<JobOfferRecord> fetchOffers(SearchContext context) {
        // Remotive to portal TYLKO zdalny. Jeśli kontekst nie dopuszcza pracy zdalnej, nie ma sensu pytać.
        if (!context.remoteOnly()) {
            return Collections.emptyList();
        }

        log.info("🚀 Pobieranie z Remotive (tylko zdalne)...");

        try {
            String uri = UriComponentsBuilder.fromPath("/remote-jobs")
                    .queryParam("category", "software-dev")
                    .toUriString();

            RemotiveResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(RemotiveResponse.class);

            if (response == null || response.getJobs() == null) {
                return Collections.emptyList();
            }

            return response.getJobs().stream()
                    .map(mapper::toDomain)
                    .filter(locationGuard::isAccepted)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("❌ Błąd Remotive: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
