package com.workdone.backend;

import com.workdone.backend.profile.service.CvAggregationService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    @Bean
    @Profile("!test")
    CommandLineRunner test(ChatClient.Builder builder) {
        return args -> {
            System.out.println("BUILDER: " + builder);

            ChatClient client = builder.build();

            String response = client.prompt()
                    .user("Say hello in one sentence")
                    .call()
                    .content();

            System.out.println("AI RESPONSE: " + response);
        };
    }

    @Bean
    CommandLineRunner testCv(CvAggregationService service) {
        return args -> {
            String profile = service.buildMergedProfile();
            System.out.println("CV PROFILE:\n" + profile);
        };
    }
}