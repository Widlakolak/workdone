package com.workdone.backend;

import com.workdone.backend.ingestion.rss.RssProperties;
import com.workdone.backend.notification.DiscordNotifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan(basePackageClasses = { BackendApplication.class, RssProperties.class })
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    @Bean
    @Profile("!test")
    CommandLineRunner sendStartNotification(DiscordNotifier notifier) {
        return args -> {
            notifier.sendControlPanel();
        };
    }
}
