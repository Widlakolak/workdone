package com.workdone.backend.ingestion.justjoinit;

import com.workdone.backend.ingestion.JobProvider;
import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.model.OfferStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
@EnableConfigurationProperties(JustJoinItProperties.class)
@ConditionalOnProperty(prefix = "workdone.providers.justjoinit", name = "enabled", havingValue = "true")
public class JustJoinItFetcher implements JobProvider {

    private static final String SOURCE_NAME = "JUST_JOIN_IT";

    private final RestClient restClient;
    private final JustJoinItProperties properties;
    private final JustJoinItLocationPolicy locationPolicy;

    public JustJoinItFetcher(@Qualifier("workDoneRestClientBuilder") RestClient.Builder restClientBuilder,
                             JustJoinItProperties properties,
                             JustJoinItLocationPolicy locationPolicy) {
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
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
        JustJoinItOfferResponse[] response = restClient.get()
                .uri(properties.offersPath())
                .retrieve()
                .body(JustJoinItOfferResponse[].class);

        if (response == null || response.length == 0) {
            return Collections.emptyList();
        }

        return Arrays.stream(response)
                .filter(Objects::nonNull)
                .filter(this::isAcceptedLocation)
                .filter(this::hasRequiredKeywords)
                .map(this::toDomainRecord)
                .toList();
    }

    private boolean isAcceptedLocation(JustJoinItOfferResponse offer) {
        if (!locationPolicy.isAccepted(offer.city(), offer.workplaceType())) {
            return false;
        }

        JustJoinItProperties.Filters filters = properties.filters();
        if (filters == null) {
            return true;
        }

        String normalizedType = normalize(offer.workplaceType());
        if ("remote".equals(normalizedType) && !filters.allowRemote()) {
            return false;
        }
        if ("hybrid".equals(normalizedType) && !filters.allowHybrid()) {
            return false;
        }

        List<String> allowedCities = filters.allowedCities();
        if (allowedCities == null || allowedCities.isEmpty() || "remote".equals(normalizedType)) {
            return true;
        }

        String normalizedCity = normalize(offer.city());
        return allowedCities.stream()
                .map(this::normalize)
                .anyMatch(normalizedCity::contains);
    }

    private boolean hasRequiredKeywords(JustJoinItOfferResponse offer) {
        JustJoinItProperties.Filters filters = properties.filters();
        if (filters == null || filters.keywords() == null || filters.keywords().isEmpty()) {
            return true;
        }

        String searchable = normalize(offer.title() + " " + String.join(" ", extractSkills(offer.skills())));

        return filters.keywords().stream()
                .map(this::normalize)
                .allMatch(searchable::contains);
    }

    private JobOfferRecord toDomainRecord(JustJoinItOfferResponse source) {
        return new JobOfferRecord(
                source.id(),
                null,
                source.title(),
                source.companyName(),
                source.url(),
                source.city(),
                "",
                "",
                extractSkills(source.skills()),
                null,
                OfferStatus.NEW,
                parsePublishedAt(source.publishedAt()),
                SOURCE_NAME
        );
    }

    private List<String> extractSkills(List<JustJoinItSkillResponse> skills) {
        if (skills == null || skills.isEmpty()) {
            return List.of();
        }
        return skills.stream()
                .map(JustJoinItSkillResponse::name)
                .filter(Objects::nonNull)
                .toList();
    }

    private LocalDateTime parsePublishedAt(String publishedAt) {
        if (publishedAt == null || publishedAt.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(publishedAt).toLocalDateTime();
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String basic = value
                .replace('ł', 'l')
                .replace('Ł', 'L');
        return Normalizer.normalize(basic, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^\\p{Alnum}]+", " ")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }
}