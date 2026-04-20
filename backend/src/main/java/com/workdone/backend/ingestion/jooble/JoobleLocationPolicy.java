package com.workdone.backend.ingestion.jooble;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
class JoobleLocationPolicy {

    private static final String LODZ = "łódź";
    private static final Set<String> HYBRID_ALLOWED = Set.of("warszawa", "poznań", "wrocław", "kraków", LODZ);

    boolean isAccepted(String location, String workplaceType) {
        String normalizedType = normalize(workplaceType);
        if ("remote".equals(normalizedType)) {
            return true;
        }

        String normalizedCity = normalize(location);
        if ("office".equals(normalizedType)) {
            return normalizedCity.contains(LODZ);
        }

        if ("hybrid".equals(normalizedType)) {
            return HYBRID_ALLOWED.stream().anyMatch(normalizedCity::contains);
        }

        return normalizedCity.contains(LODZ);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}