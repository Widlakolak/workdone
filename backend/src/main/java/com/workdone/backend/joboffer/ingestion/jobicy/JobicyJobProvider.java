package com.workdone.backend.joboffer.ingestion.jobicy;

import com.workdone.backend.joboffer.analysis.LocationGuard;
import com.workdone.backend.joboffer.ingestion.JobProvider;
import com.workdone.backend.joboffer.ingestion.SearchContext;
import com.workdone.backend.common.model.JobOfferRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

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
        log.info("🚀 [JOBICY] Pobieram paczkę ofert dla lokalizacji: {}", context.location());

        try {
            JobicyResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("count", 50)
                            .queryParam("geo", "europe")
                            .build())
                    .retrieve()
                    .body(JobicyResponse.class);

            if (response == null || response.getJobs() == null) return List.of();

            return response.getJobs().stream()
                    .map(mapper::toDomain)
                    .filter(offer -> matchesKeywords(offer, context.keywords()))
                    .filter(locationGuard::isAccepted)
                    .toList();

        } catch (Exception e) {
            log.error("❌ Jobicy error: {}", e.getMessage());
            return List.of();
        }
    }

    private boolean matchesKeywords(JobOfferRecord offer, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return true;
        String content = (offer.title() + " " + offer.rawDescription()).toLowerCase();
        return keywords.stream().anyMatch(k -> content.contains(k.toLowerCase()));
    }

//    private List<JobOfferRecord> fetchForTag(String tag, SearchContext context) {
//        try {
//            JobicyResponse response = restClient.get()
//                    .uri(uriBuilder -> uriBuilder
//                            .queryParam("count", context.maxResults())
//                            .queryParam("tag", tag)
//                            .queryParam("geo", context.location() != null ? context.location() : "")
//                            .build())
//                    .retrieve()
//                    .body(JobicyResponse.class);
//
//            if (response == null || !response.isSuccess() || response.getJobs() == null) {
//                return Collections.emptyList();
//            }
//
//            return response.getJobs().stream()
//                    .map(mapper::toDomain)
//                    .collect(Collectors.toList());
//
//        } catch (Exception e) {
//            log.error("❌ Błąd przy pobieraniu z Jobicy (tag {}): {}", tag, e.getMessage());
//            return Collections.emptyList();
//        }
//    }
}
