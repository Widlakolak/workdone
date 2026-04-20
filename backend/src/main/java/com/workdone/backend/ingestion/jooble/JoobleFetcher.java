package com.workdone.backend.ingestion.jooble;

import com.fasterxml.jackson.databind.ObjectMapper; // Pamiętaj o imporcie!
import com.workdone.backend.ingestion.JobProvider;
import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.model.OfferStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.apache.kafka.common.utils.Utils.safe;

@Component
@EnableConfigurationProperties(JoobleProperties.class)
@ConditionalOnProperty(prefix = "workdone.providers.jooble", name = "enabled", havingValue = "true")
public class JoobleFetcher implements JobProvider {

    private static final String SOURCE_NAME = "JOOBLE";

    private final RestClient restClient;
    private final JoobleProperties properties;
    private final JoobleLocationPolicy locationPolicy;
    private final ObjectMapper objectMapper= new ObjectMapper();

    public JoobleFetcher(@Qualifier("workDoneRestClientBuilder") RestClient.Builder restClientBuilder,
                         JoobleProperties properties,
                         JoobleLocationPolicy locationPolicy) {
        this.restClient = restClientBuilder
                .baseUrl(properties.url())
                .build();
        this.properties = properties;
        this.locationPolicy = locationPolicy;
    }

    @Override
    public String sourceName() {
        return SOURCE_NAME;
    }

    @Override
    public List<JobOfferRecord> fetchOffers() {
        try {
            String apiKey = properties.apiKey();
            var requestBody = Map.of(
                    "keywords", String.join(" ", properties.filters().keywords()),
                    "location", properties.filters().location() != null ? properties.filters().location() : "Łódź",
                    "remote", properties.filters().allowRemote()
            );

            System.out.println("\n--- [JOOBLE START] ---");
            System.out.println("API URL: " + properties.url() + apiKey);
            System.out.println("PAYLOAD: " + requestBody);

            // Pobieramy surowy tekst
            String rawJson = restClient.post()
                    .uri("/{apiKey}", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            System.out.println("SUROWY JSON Z JOOBLE: " + rawJson);

            // Ręczna deserializacja
            JoobleResponse response = objectMapper.readValue(rawJson, JoobleResponse.class);

            if (response == null || response.jobs() == null || response.jobs().isEmpty()) {
                System.out.println("WYNIK: Jooble nie zwróciło żadnych ofert dla tych kryteriów.");
                return Collections.emptyList();
            }

            System.out.println("ZNALEZIONO (RAW): " + response.jobs().size());

            return response.jobs().stream()
                    .peek(job -> System.out.println("Analizuję ofertę: " + job.title() + " | Lokalizacja: " + job.location() + " | Typ: " + job.type()))
                    .filter(job -> {
                        boolean accepted = isAccepted(job);
                        if (!accepted) {
                            System.out.println("   [X] ODRZUCONO przez lokalizację=" + job.location());
                        } else {
                            System.out.println("   [V] ZAAKCEPTOWANO: " + job.title());
                        }
                        return accepted;
                    })
                    .map(this::toDomainRecord)
                    .toList();

        } catch (Exception e) {
            System.err.println("--- [JOOBLE ERROR] ---");
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private boolean isAccepted(JoobleResponse.JoobleJob job) {
        String location = job.location() == null ? "" : job.location().toLowerCase();

        boolean isRemote = location.contains("remote") || location.contains("zdalnie");

        if (isRemote) {
            return properties.filters().allowRemote();
        }

        if (location.equals("poland") || location.isBlank()) {
            return true;
        }

        return locationPolicy.isAccepted(job.location(), job.type());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private List<String> extractTechStack(JoobleResponse.JoobleJob job) {

        String text = (safe(job.title()) + " " + safe(job.snippet())).toLowerCase();

        List<String> knownTech = List.of(
                "java", "spring", "spring boot", "hibernate", "jpa",
                "sql", "postgres", "mysql",
                "docker", "docker-compose", "kubernetes",
                "aws", "azure",
                "rest", "microservices"
        );

        return knownTech.stream()
                .filter(text::contains)
                .toList();
    }

    private LocalDateTime parseDate(String updated) {
        try {
            return updated != null ? LocalDateTime.parse(updated) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private JobOfferRecord toDomainRecord(JoobleResponse.JoobleJob job) {

        List<String> techStack = extractTechStack(job);

        return new JobOfferRecord(
                UUID.randomUUID().toString(),
                null,
                safe(job.title()),
                safe(job.company()),
                safe(job.link()),
                safe(job.location()),
                safe(job.snippet()),
                job.salary() != null ? job.salary() : "",
                techStack,
                null,
                null,
                OfferStatus.NEW,
                parseDate(job.updated()),
                SOURCE_NAME
        );
    }
}