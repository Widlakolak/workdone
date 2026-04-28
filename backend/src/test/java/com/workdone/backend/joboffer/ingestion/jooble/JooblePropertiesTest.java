package com.workdone.backend.joboffer.ingestion.jooble;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JooblePropertiesTest {

    @Test
    void shouldFallbackToBaseUrlWhenUrlMissing() {
        JoobleProperties.Filters filters = new JoobleProperties.Filters(List.of("java"), "Poland", true);
        JoobleProperties properties = new JoobleProperties(true, "key", null, "https://jooble.org/api", filters);

        assertThat(properties.resolvedUrl()).isEqualTo("https://jooble.org/api");
    }
}