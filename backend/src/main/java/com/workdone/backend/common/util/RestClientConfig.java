package com.workdone.backend.common.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean("workDoneRestClientBuilder")
    RestClient.Builder workDoneRestClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(6_000);
        requestFactory.setReadTimeout(12_000);

        // Konfiguruję bazowy klient HTTP. Dodaję User-Agent, bo niektóre portale z ofertami
        // (np. Jobicy) blokują gołe requesty bez nagłówka przeglądarki/bota.
        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, "WorkDone-Bot/1.0");
    }
}