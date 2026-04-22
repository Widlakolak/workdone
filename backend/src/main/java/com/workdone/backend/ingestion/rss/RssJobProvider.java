package com.workdone.backend.ingestion.rss;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.workdone.backend.analysis.LocationGuard;
import com.workdone.backend.ingestion.JobProvider;
import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.model.OfferStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Pobieracz ofert z kanałów RSS. 
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "workdone.providers.rss", name = "enabled", havingValue = "true")
public class RssJobProvider implements JobProvider {

    private final RssProperties properties;
    private final LocationGuard locationGuard;

    @Override
    public String sourceName() {
        return "RSS_AGGREGATOR";
    }

    @Override
    public List<JobOfferRecord> fetchOffers() {
        List<JobOfferRecord> allOffers = new ArrayList<>();

        for (RssProperties.RssSource source : properties.sources()) {
            try {
                log.info("🔍 Pobieranie RSS z: {}...", source.name());
                
                HttpClient client = HttpClient.newHttpClient();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(source.url()))
                        .header("User-Agent", "Mozilla/5.0")
                        .header("Accept", "application/rss+xml, application/xml")
                        .GET()
                        .build();

                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

                InputStream cleanedStream = removeDoctype(response.body());
                SyndFeed feed = new SyndFeedInput().build(new XmlReader(cleanedStream));

                List<JobOfferRecord> sourceOffers = feed.getEntries().stream()
                        .map(entry -> toRecord(entry, source.name()))
                        .filter(offer -> {
                            // Tu sprawdzam czy lokalizacja nam pasuje, żeby nie śmiecić AI ofertami z dupy
                            boolean accepted = locationGuard.isAccepted(offer);
                            if (!accepted) {
                                log.trace("📍 Odrzucono ofertę RSS ze względu na lokalizację: {} ({})", offer.title(), offer.location());
                            }
                            return accepted;
                        })
                        .toList();

                allOffers.addAll(sourceOffers);
                log.info("✅ Pomyślnie pobrano {} ofert z {}", sourceOffers.size(), source.name());

            } catch (Exception e) {
                log.error("❌ Błąd pobierania RSS z {}: {}", source.name(), e.getMessage());
            }
        }

        return allOffers;
    }

    private JobOfferRecord toRecord(SyndEntry entry, String platform) {
        String cleanTitle = entry.getTitle() != null ? entry.getTitle().replaceAll("\\s+", " ").trim() : "Bez tytułu";
        
        return JobOfferRecord.builder()
                .id(UUID.randomUUID().toString())
                .title(cleanTitle)
                .companyName(extractCompany(entry, platform))
                .sourceUrl(entry.getLink())
                .location("Remote") // Zazwyczaj tak jest w moich źródłach, ale LocationGuard to sprawdzi
                .rawDescription(entry.getDescription() != null ? entry.getDescription().getValue() : "")
                .techStack(Collections.emptyList())
                .status(OfferStatus.NEW)
                .publishedAt(entry.getPublishedDate() != null ? 
                        entry.getPublishedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null)
                .sourcePlatform(platform)
                .build();
    }

    private String extractCompany(SyndEntry entry, String platform) {
        if (entry.getAuthor() != null && !entry.getAuthor().isBlank()) {
            return entry.getAuthor().trim();
        }
        return platform;
    }

    private InputStream removeDoctype(InputStream input) throws IOException {
        String xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        xml = xml.replaceAll("<!DOCTYPE[^>]*>", "");
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }
}
