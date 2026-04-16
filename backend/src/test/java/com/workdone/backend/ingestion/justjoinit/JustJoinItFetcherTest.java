package com.workdone.backend.ingestion.justjoinit;

import com.workdone.backend.model.JobOfferRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JustJoinItFetcherTest {

    private MockRestServiceServer mockServer;
    private JustJoinItFetcher fetcher;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        this.mockServer = MockRestServiceServer.bindTo(builder).build();

        fetcher = new JustJoinItFetcher(
                builder,
                new JustJoinItProperties(
                        true,
                        "https://api.justjoin.it",
                        "/v2/user-panel/offers",
                        "HIGH",
                        new JustJoinItProperties.Filters(
                                List.of("java", "spring"),
                                List.of("lodz"),
                                true,
                                true
                        )
                ),
                new JustJoinItLocationPolicy()
        );
    }

    @Test
    void shouldFetchAndMapOffersMatchingLocationAndKeywords() {
        mockServer.expect(MockRestRequestMatchers.requestTo("https://api.justjoin.it/v2/user-panel/offers"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(MockRestResponseCreators.withSuccess("""
                        [
                          {
                            "id": "1",
                            "title": "Java Developer",
                            "companyName": "ACME",
                            "city": "Łódź",
                            "workplaceType": "office",
                            "skills": [{"name": "Java"}, {"name": "Spring"}],
                            "publishedAt": "2026-04-15T10:00:00+02:00",
                            "url": "https://justjoin.it/offers/acme-java"
                          },
                          {
                            "id": "2",
                            "title": "Senior Java Developer",
                            "companyName": "GlobalCorp",
                            "city": "Tokyo",
                            "workplaceType": "office",
                            "skills": [{"name": "Java"}],
                            "publishedAt": "2026-04-15T10:00:00+02:00",
                            "url": "https://justjoin.it/offers/globalcorp-java"
                          },
                          {
                            "id": "3",
                            "title": "Backend Developer",
                            "companyName": "RemoteWorks",
                            "city": "Anywhere",
                            "workplaceType": "remote",
                            "skills": [{"name": "Kotlin"}],
                            "publishedAt": "2026-04-15T10:00:00+02:00",
                            "url": "https://justjoin.it/offers/remoteworks-backend"
                          }
                        ]
                        """, org.springframework.http.MediaType.APPLICATION_JSON));

        List<JobOfferRecord> offers = fetcher.fetchOffers();

        assertThat(offers).hasSize(1);
        assertThat(offers)
                .extracting(JobOfferRecord::id)
                .containsExactly("1");
        assertThat(offers.getFirst().techStack()).containsExactly("Java", "Spring");
        assertThat(offers.getFirst().sourcePlatform()).isEqualTo("JUST_JOIN_IT");

        mockServer.verify();
    }
}