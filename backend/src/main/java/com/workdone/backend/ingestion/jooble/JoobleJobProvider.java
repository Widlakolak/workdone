package com.workdone.backend.ingestion.jooble;

import com.workdone.backend.analysis.LocationGuard;
import com.workdone.backend.ingestion.JobProvider;
import com.workdone.backend.ingestion.JobSearchParametersProvider;
import com.workdone.backend.ingestion.SearchContext;
import com.workdone.backend.model.JobOfferRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
    private final JobSearchParametersProvider searchParametersProvider;
    private final LocationGuard locationGuard;

    public JoobleJobProvider(@Qualifier("workDoneRestClientBuilder") RestClient.Builder restClientBuilder,
                             JoobleProperties properties,
                             JoobleMapper mapper,
                             JobSearchParametersProvider searchParametersProvider,
                             LocationGuard locationGuard) {
        this.restClient = restClientBuilder
                .baseUrl(properties.url())
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
        try {
            // Bez klucza API do Jooble nic nie zdziałamy
            String apiKey = properties.apiKey();
            if (apiKey == null || apiKey.isBlank()) {
                log.error("❌ Brak klucza API Jooble!");
                return Collections.emptyList();
            }

            // Pobieram aktualny kontekst (słowa kluczowe, moją lokalizację itp.)
            SearchContext context = searchParametersProvider.getContext();
            
            var requestBody = Map.of(
                    "keywords", context.getQueryString(),
                    "location", context.location(),
                    "remote", context.remoteOnly()
            );

            log.info("🔍 Wysyłam zapytanie do Jooble API: {}", requestBody);

            JoobleResponse response = restClient.post()
                    .uri("/" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(JoobleResponse.class);

            if (response == null || response.jobs() == null || response.jobs().isEmpty()) {
                log.info("ℹ️ Jooble - brak nowych ofert.");
                return Collections.emptyList();
            }

            // Mapuję wyniki na mój wewnętrzny format i przepuszczam przez LocationGuard, 
            // żeby odsiać oferty z miast, które mnie nie interesują.
            List<JobOfferRecord> offers = response.jobs().stream()
                    .map(mapper::toDomain)
                    .filter(locationGuard::isAccepted)
                    .toList();

            log.info("📊 Jooble Summary: Znaleziono {}, Zaakceptowano {} po filtracji lokalizacji", response.jobs().size(), offers.size());
            return offers;

        } catch (Exception e) {
            log.error("❌ Jooble Fetch Error: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
