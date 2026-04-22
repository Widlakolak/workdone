package com.workdone.backend.ingestion.remotive;

import com.workdone.backend.analysis.LocationGuard;
import com.workdone.backend.ingestion.JobProvider;
import com.workdone.backend.ingestion.JobSearchParametersProvider;
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
    private final JobSearchParametersProvider searchParametersProvider;
    private final LocationGuard locationGuard;

    public RemotiveJobProvider(@Qualifier("workDoneRestClientBuilder") RestClient.Builder restClientBuilder,
                               RemotiveProperties properties,
                               RemotiveMapper mapper,
                               JobSearchParametersProvider searchParametersProvider,
                               LocationGuard locationGuard) {
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .build();
        this.properties = properties;
        this.mapper = mapper;
        this.searchParametersProvider = searchParametersProvider;
        this.locationGuard = locationGuard;
    }

    @Override
    public String sourceName() {
        return SOURCE_NAME;
    }

    @Override
    public List<JobOfferRecord> fetchOffers() {
        SearchContext context = searchParametersProvider.getContext();
        log.info("🚀 Zaczynam pobieranie z Remotive dla słów kluczowych: {}", context.keywords());

        try {
            // Remotive API nie ma bezpośredniego wyszukiwania po słowach kluczowych w tytule/opisie
            // Możemy filtrować po kategorii, ale to wymagałoby mapowania naszych słów kluczowych na kategorie Remotive.
            // Na razie pobieramy wszystko i filtrujemy lokalnie.
            // Możemy też użyć parametru 'search', ale to jest dostępne tylko dla płatnego API.
            // Na potrzeby darmowego API, pobieramy po prostu kategorię 'software-dev'
            // lub wszystkie, jeśli nie ma konkretnej kategorii.

            String uri = UriComponentsBuilder.fromPath("/remote-jobs")
                    .queryParam("category", "software-dev") // Zgodnie z przykładem z dokumentacji
                    .toUriString();

            log.debug("Wysyłam request do Remotive API: {}", uri);

            RemotiveResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(RemotiveResponse.class);

            if (response == null || response.getJobs() == null) {
                log.warn("⚠️ Remotive nic nie zwróciło (albo błąd)");
                return Collections.emptyList();
            }

            List<JobOfferRecord> allOffers = response.getJobs().stream()
                    .map(mapper::toDomain)
                    .filter(locationGuard::isAccepted)
                    .collect(Collectors.toList());

            log.info("📊 Remotive Summary: Znaleziono {}, Zaakceptowano {} po filtracji lokalizacji", response.getJobs().size(), allOffers.size());
            return allOffers;

        } catch (Exception e) {
            log.error("❌ Błąd przy pobieraniu z Remotive: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
