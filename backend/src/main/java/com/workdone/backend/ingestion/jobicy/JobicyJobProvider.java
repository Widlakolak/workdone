package com.workdone.backend.ingestion.jobicy;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@EnableConfigurationProperties(JobicyProperties.class)
@ConditionalOnProperty(prefix = "workdone.providers.jobicy", name = "enabled", havingValue = "true")
public class JobicyJobProvider implements JobProvider {

    private static final String SOURCE_NAME = "JOBICY";
    
    private final RestClient restClient;
    private final JobicyProperties properties;
    private final JobicyMapper mapper;
    private final JobSearchParametersProvider searchParametersProvider;
    private final LocationGuard locationGuard;

    public JobicyJobProvider(@Qualifier("workDoneRestClientBuilder") RestClient.Builder restClientBuilder,
                             JobicyProperties properties,
                             JobicyMapper mapper,
                             JobSearchParametersProvider searchParametersProvider,
                             LocationGuard locationGuard) {
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl()) // Używamy baseUrl z properties
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
        log.info("🚀 Zaczynam pobieranie z Jobicy dla słów kluczowych: {}", context.keywords());

        List<JobOfferRecord> allOffers = new ArrayList<>();
        
        // Jobicy najlepiej działa przy szukaniu po tagach. 
        // Biorę top 3 słowa kluczowe, żeby nie spamować im serwerów zbyt wieloma zapytaniami.
        List<String> topKeywords = context.keywords().stream().limit(3).toList();

        for (String tag : topKeywords) {
            allOffers.addAll(fetchForTag(tag, context));
        }

        // Wywalam duplikaty (bo jedna oferta może pasować do kilku tagów) i filtruję lokalizację
        List<JobOfferRecord> filtered = allOffers.stream()
                .distinct()
                .filter(locationGuard::isAccepted)
                .toList();

        log.info("📊 Jobicy Summary: Znaleziono {}, Zaakceptowano {} po filtracji lokalizacji", allOffers.size(), filtered.size());
        return filtered;
    }

    private List<JobOfferRecord> fetchForTag(String tag, SearchContext context) {
        try {
            // Używamy funkcji UriBuilder do budowania URI, co jest bezpieczniejsze z RestClient
            JobicyResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("count", context.maxResults())
                            .queryParam("tag", tag)
                            .build())
                    .retrieve()
                    .body(JobicyResponse.class);

            if (response == null || !response.isSuccess() || response.getJobs() == null) {
                log.warn("⚠️ Jobicy nic nie zwróciło (albo błąd) dla tagu: {}", tag);
                return Collections.emptyList();
            }

            return response.getJobs().stream()
                    .map(mapper::toDomain)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("❌ Błąd przy pobieraniu z Jobicy (tag {}): {}", tag, e.getMessage());
            return Collections.emptyList();
        }
    }
}
