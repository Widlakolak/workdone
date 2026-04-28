package com.workdone.backend.joboffer.ingestion.jooble;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workdone.backend.common.util.ProviderCallExecutor;
import com.workdone.backend.joboffer.analysis.DynamicConfigService;
import com.workdone.backend.joboffer.analysis.LocationGuard;
import com.workdone.backend.joboffer.analysis.TechStackExtractor;
import com.workdone.backend.joboffer.ingestion.SearchContext;
import com.workdone.backend.common.model.JobOfferRecord;
import com.workdone.backend.common.model.LocationPolicy;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class JoobleJobProviderTest {

    private JoobleJobProvider joobleProvider;
    private MockRestServiceServer mockServer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JoobleMapper joobleMapper = Mappers.getMapper(JoobleMapper.class);
    private final DynamicConfigService dynamicConfigService = Mockito.mock(DynamicConfigService.class);
    private final LocationGuard locationGuard = new LocationGuard(dynamicConfigService);
    private final TechStackExtractor techStackExtractor = new TechStackExtractor(); // Prawdziwy ekstraktor, bo jest bezstanowy
    private final ProviderCallExecutor callExecutor = Mockito.mock(ProviderCallExecutor.class);

    @BeforeEach
    void setUp() {
        // Ręcznie wstrzykujemy zależność do mapera MapStructa, bo w teście jednostkowym Spring go nie zestawia
        ReflectionTestUtils.setField(joobleMapper, "techStackExtractor", techStackExtractor);

        JoobleProperties.Filters filters = new JoobleProperties.Filters(List.of("java"), "Lodz", true);
        JoobleProperties properties = new JoobleProperties(true, "dummy-key-123", "https://jooble.org/api/", null, filters);
        
        when(dynamicConfigService.getLocationPolicies()).thenReturn(List.of(new LocationPolicy("Lodz", true, true, true, 5)));

        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

        joobleProvider = new JoobleJobProvider(restClientBuilder, properties, joobleMapper, locationGuard, callExecutor);
    }

    @Test
    void shouldFailFastWhenEnabledAndUrlMissing() {
        JoobleProperties.Filters filters = new JoobleProperties.Filters(List.of("java"), "Lodz", true);
        JoobleProperties broken = new JoobleProperties(true, "dummy-key-123", null, null, filters);

        JoobleJobProvider provider = new JoobleJobProvider(RestClient.builder(), broken, joobleMapper, locationGuard, callExecutor);

        assertThatThrownBy(provider::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("URL is missing");
    }

    @Test
    void shouldFetchAndFilterOffersFromJooble() throws Exception {
        // Given
        SearchContext context = SearchContext.builder()
                .keywords(List.of("java"))
                .location("Lodz")
                .remoteOnly(true)
                .build();

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

        when(callExecutor.execute(Mockito.eq("JOOBLE"), Mockito.any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.concurrent.Callable<JoobleResponse> callable = invocation.getArgument(1);
            return callable.call();
        });

        // When
        List<JobOfferRecord> offers = joobleProvider.fetchOffers(context);

        // Then
        mockServer.verify();
        assertThat(offers).hasSize(1);
        assertThat(offers.get(0).location()).isEqualTo("Lodz");
        // Sprawdzam czy tech stack się wyciągnął, żeby mieć pewność że maper działa
        assertThat(offers.get(0).techStack()).contains("java");
    }
}
