package com.workdone.backend.orchestration;

import com.workdone.backend.analysis.MatchingBand;
import com.workdone.backend.analysis.OfferClassificationService;
import com.workdone.backend.analysis.OfferFingerprintFactory;
import com.workdone.backend.analysis.OfferMatchingService;
import com.workdone.backend.config.WorkDoneProperties;
import com.workdone.backend.ingestion.JobProvider;
import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.notification.DiscordNotifier;
import com.workdone.backend.storage.InMemoryOfferStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Component
public class OfferIngestionOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OfferIngestionOrchestrator.class);

    private final List<JobProvider> providers;
    private final OfferFingerprintFactory fingerprintFactory;
    private final OfferMatchingService matchingService;
    private final OfferClassificationService classificationService;
    private final InMemoryOfferStore store;
    private final DiscordNotifier notifier;
    private final WorkDoneProperties properties;

    public OfferIngestionOrchestrator(List<JobProvider> providers,
                                      OfferFingerprintFactory fingerprintFactory,
                                      OfferMatchingService matchingService,
                                      OfferClassificationService classificationService,
                                      InMemoryOfferStore store,
                                      DiscordNotifier notifier,
                                      WorkDoneProperties properties) {
        this.providers = providers;
        this.fingerprintFactory = fingerprintFactory;
        this.matchingService = matchingService;
        this.classificationService = classificationService;
        this.store = store;
        this.notifier = notifier;
        this.properties = properties;
    }

    @Scheduled(cron = "${workdone.scheduling.ingestion-cron}", zone = "${workdone.scheduling.zone-id}")
    public void runIngestion() {
        for (JobProvider provider : providers) {
            List<JobOfferRecord> offers = provider.fetchOffers().stream()
                    .map(this::enrichIdentity)
                    .filter(offer -> !store.existsBySourceOrFingerprint(offer))
                    .toList();

            for (JobOfferRecord offer : offers) {
                if (!matchingService.passesMustHave(offer)) {
                    continue;
                }

                double score = matchingService.score(offer);
                MatchingBand band = classificationService.classify(score);
                JobOfferRecord analyzed = offer.withAnalysis(score, classificationService.toStatus(band));

                store.upsert(analyzed);
                if (band == MatchingBand.INSTANT) {
                    notifier.sendInstant(analyzed);
                }
            }

            log.info("Provider={} pobrane={}", provider.sourceName(), offers.size());
        }
    }

    @Scheduled(cron = "${workdone.scheduling.digest-cron}", zone = "${workdone.scheduling.zone-id}")
    public void sendDailyDigest() {
        LocalDate today = LocalDate.now(ZoneId.of(properties.scheduling().zoneId()));
        List<JobOfferRecord> digestOffers = store.findForDigest(today).stream()
                .filter(offer -> offer.matchingScore() != null)
                .filter(offer -> offer.matchingScore() >= properties.matching().digestThreshold())
                .filter(offer -> offer.matchingScore() < properties.matching().instantThreshold())
                .toList();
        notifier.sendDigest(digestOffers);
    }

    private JobOfferRecord enrichIdentity(JobOfferRecord offer) {
        String id = offer.id() == null || offer.id().isBlank() ? UUID.randomUUID().toString() : offer.id();
        String fingerprint = fingerprintFactory.createFrom(offer.title(), offer.companyName(), offer.location());

        return new JobOfferRecord(
                id,
                fingerprint,
                offer.title(),
                offer.companyName(),
                offer.sourceUrl(),
                offer.location(),
                offer.rawDescription(),
                offer.salaryRange(),
                offer.techStack(),
                offer.matchingScore(),
                offer.status(),
                offer.publishedAt(),
                offer.sourcePlatform()
        );
    }
}