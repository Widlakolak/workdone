package com.workdone.backend.ingestion.jooble;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workdone.backend.analysis.DynamicConfigService;
import com.workdone.backend.analysis.LocationGuard;
import com.workdone.backend.analysis.TechStackExtractor;
import com.workdone.backend.ingestion.JobSearchParametersProvider;
import com.workdone.backend.ingestion.SearchContext;
import com.workdone.backend.model.JobOfferRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class JoobleJobProviderTest {

    private JoobleJobProvider joobleProvider;
    private MockRestServiceServer mockServer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JoobleMapper joobleMapper = Mappers.getMapper(JoobleMapper.class);
    private final JobSearchParametersProvider searchProvider = Mockito.mock(JobSearchParametersProvider.class);
    private final DynamicConfigService dynamicConfigService = Mockito.mock(DynamicConfigService.class);
    private final LocationGuard locationGuard = new LocationGuard(dynamicConfigService);
    private final TechStackExtractor techStackExtractor = new TechStackExtractor(); // Prawdziwy ekstraktor, bo jest bezstanowy

    @BeforeEach
    void setUp() {
        // Ręcznie wstrzykujemy zależność do mapera MapStructa, bo w teście jednostkowym Spring go nie zestawia
        ReflectionTestUtils.setField(joobleMapper, "techStackExtractor", techStackExtractor);

        JoobleProperties.Filters filters = new JoobleProperties.Filters(List.of("java"), "Lodz", true);
        JoobleProperties properties = new JoobleProperties(true, "dummy-key-123", "https://jooble.org/api/", filters);
        
        SearchContext context = SearchContext.builder()
                .keywords(List.of("java"))
                .location("Lodz")
                .remoteOnly(true)
                .build();
        
        when(searchProvider.getContext()).thenReturn(context);
        when(dynamicConfigService.getPreferredLocation()).thenReturn("Lodz");

        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

        joobleProvider = new JoobleJobProvider(restClientBuilder, properties, joobleMapper, searchProvider, locationGuard);
    }

    @Test
    void shouldFetchAndFilterOffersFromJooble() throws Exception {
        // Given
        JoobleResponse.JoobleJob acceptedJob = new JoobleResponse.JoobleJob(
                "1", "Java Dev", "Firm", "Lodz", "Desc", "10k", "url1", "Jooble", "remote", "2026-04-18"
        );
        JoobleResponse.JoobleJob rejectedJob = new JoobleResponse.JoobleJob(
                "2", "Java Dev", "Firm", "Berlin", "Office only", "10k", "url2", "Jooble", "office", "2026-04-18"
        );
        JoobleResponse mockResponse = new JoobleResponse(2, List.of(acceptedJob, rejectedJob));

        mockServer.expect(requestTo("https://jooble.org/api/dummy-key-123"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        // When
        List<JobOfferRecord> offers = joobleProvider.fetchOffers();

        // Then
        mockServer.verify();
        assertThat(offers).hasSize(1);
        assertThat(offers.get(0).location()).isEqualTo("Lodz");
        // Sprawdzam czy tech stack się wyciągnął, żeby mieć pewność że maper działa
        assertThat(offers.get(0).techStack()).contains("java");
    }
}
