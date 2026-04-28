package com.workdone.backend.joboffer.ingestion.rss;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "workdone.providers.rss")
public record RssProperties(
        boolean enabled,
        List<RssSource> sources
) {
    public record RssSource(String name, String url) {}
}
