package com.workdone.backend.ingestion.jooble;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workdone.backend.model.JobOfferRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class JoobleFetcherTest {

    private JoobleFetcher joobleFetcher;
    private MockRestServiceServer mockServer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // 1. Definicja domenowych zależności w locie
        JoobleProperties.Filters filters = new JoobleProperties.Filters(List.of("java", "spring"), "Lodz", true);
        JoobleProperties properties = new JoobleProperties(true, "dummy-key-123", "https://jooble.org/api/", filters);
        JoobleLocationPolicy locationPolicy = new JoobleLocationPolicy();

        // 2. Przygotowanie czystego buildera, do którego wpinany jest serwer mockujący Springa
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

        // 3. Utworzenie testowanej instancji (to wywoła pod spodem .build() budując lokalnego, ale zmockowanego klienta)
        joobleFetcher = new JoobleFetcher(restClientBuilder, properties, locationPolicy);
    }

    @Test
    void shouldFetchAndMapOffersFromJooble() throws Exception {
        // Given
        JoobleResponse.JoobleJob sampleJob = new JoobleResponse.JoobleJob(
                "12345",
                "Senior Java Developer (Spring Boot)",
                "TechCorp Sp. z o.o.",
                "Lodz (hybrydowo)",
                "Doświadczenie z Spring Boot...",
                "10000 - 15000 PLN",
                "https://example.com/offer/12345",
                "Jooble",
                "remote",
                "2026-04-18"
        );
        JoobleResponse mockResponse = new JoobleResponse(1, List.of(sampleJob));

        // Konfiguracja kontraktu: Czego oczekujemy na wyjściu klienta HTTP i co zwracamy w payloadzie
        mockServer.expect(requestTo("https://jooble.org/api/dummy-key-123"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        // When
        List<JobOfferRecord> offers = joobleFetcher.fetchOffers();

        // Then
        mockServer.verify(); // Kluczowe: sprawdza czy Spring wysłał precyzyjnie żądanie określone w expekcie

        assertThat(offers).isNotEmpty();
        assertThat(offers.get(0).title()).contains("Java");
        assertThat(offers.get(0).companyName()).isEqualTo("TechCorp Sp. z o.o.");
    }
}