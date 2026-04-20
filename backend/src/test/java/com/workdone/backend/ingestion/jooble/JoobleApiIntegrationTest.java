package com.workdone.backend.ingestion.jooble;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(properties = {
        "workdone.providers.jooble.apiKey=c16b537c-74e4-43d6-ad60-95a62662e835",
        "workdone.providers.jooble.url=https://jooble.org/api/",
        "workdone.providers.jooble.enabled=true",
        "workdone.providers.jooble.filters.keywords=java",
        "workdone.providers.jooble.filters.location=Europe"
})
class JoobleApiIntegrationTest {

    @Autowired
    private JoobleFetcher joobleFetcher;

    @Test
    void checkActualJoobleResponse() {
        var offers = joobleFetcher.fetchOffers();
        System.out.println("Otrzymano ofert: " + offers.size());
        assertThat(offers).isNotNull();
    }
}