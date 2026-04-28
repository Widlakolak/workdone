package com.workdone.backend.common.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean("workDoneRestClientBuilder")
    RestClient.Builder workDoneRestClientBuilder() {
        // Konfiguruję bazowy klient HTTP. Dodaję User-Agent, bo niektóre portale z ofertami 
        // (np. Jobicy) blokują gołe requesty bez nagłówka przeglądarki/bota.
        return RestClient.builder()
                .defaultHeader(HttpHeaders.USER_AGENT, "WorkDone-Bot/1.0");
    }
}