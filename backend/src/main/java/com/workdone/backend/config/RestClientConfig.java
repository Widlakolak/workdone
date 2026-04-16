package com.workdone.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean("workDoneRestClientBuilder")
    RestClient.Builder workDoneRestClientBuilder() {
        return RestClient.builder()
                .defaultHeader(HttpHeaders.USER_AGENT, "WorkDone-Bot/1.0");
    }
}