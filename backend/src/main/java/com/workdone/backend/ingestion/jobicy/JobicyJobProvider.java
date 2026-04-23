package com.workdone.backend.ingestion.jobicy;

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
    private final LocationGuard locationGuard;

    public JobicyJobProvider(@Qualifier("workDoneRestClientBuilder") RestClient.Builder restClientBuilder,
                             JobicyProperties properties,
                             JobicyMapper mapper,
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
        log.info("🚀 Zaczynam pobieranie z Jobicy dla lokalizacji: {}", context.location());

        List<JobOfferRecord> allOffers = new ArrayList<>();
        List<String> topKeywords = context.keywords().stream().limit(3).toList();

        for (String tag : topKeywords) {
            allOffers.addAll(fetchForTag(tag, context));
        }

        return allOffers.stream()
                .distinct()
                .filter(locationGuard::isAccepted)
                .toList();
    }

    private List<JobOfferRecord> fetchForTag(String tag, SearchContext context) {
        try {
            JobicyResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("count", context.maxResults())
                            .queryParam("tag", tag)
                            .queryParam("geo", context.location() != null ? context.location() : "")
                            .build())
                    .retrieve()
                    .body(JobicyResponse.class);

            if (response == null || !response.isSuccess() || response.getJobs() == null) {
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
