package com.workdone.backend.orchestration;

import com.workdone.backend.analysis.*;
import com.workdone.backend.config.WorkDoneProperties;
import com.workdone.backend.format.OfferContentBuilder;
import com.workdone.backend.ingestion.JobProvider;
import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.model.OfferStatus;
import com.workdone.backend.notification.DiscordNotifier;
import com.workdone.backend.storage.InMemoryOfferStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
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
    private final OfferAnalysisFacade analysisFacade;
    private final OfferDeduplicationService deduplicationService;
    private final OfferPriorityService priorityService;
    private final OfferContentBuilder contentBuilder;

    public OfferIngestionOrchestrator(List<JobProvider> providers,
                                      OfferFingerprintFactory fingerprintFactory,
                                      OfferMatchingService matchingService,
                                      OfferClassificationService classificationService,
                                      OfferAnalysisFacade analysisFacade,
                                      OfferDeduplicationService deduplicationService,
                                      OfferPriorityService priorityService,
                                      InMemoryOfferStore store,
                                      DiscordNotifier notifier,
                                      WorkDoneProperties properties,
                                      OfferContentBuilder contentBuilder) {
        this.providers = providers;
        this.fingerprintFactory = fingerprintFactory;
        this.matchingService = matchingService;
        this.classificationService = classificationService;
        this.store = store;
        this.notifier = notifier;
        this.properties = properties;
        this.analysisFacade = analysisFacade;
        this.deduplicationService = deduplicationService;
        this.priorityService = priorityService;
        this.contentBuilder = contentBuilder;
    }

    @Scheduled(cron = "${workdone.scheduling.ingestion-cron}", zone = "${workdone.scheduling.zone-id}")
    public void runIngestion() {
        for (JobProvider provider : providers) {
            List<JobOfferRecord> offers = provider.fetchOffers().stream()
                    .filter(offer -> !store.existsBySourceOrFingerprint(offer))
                    .map(this::enrichIdentity)
                    .toList();

            for (JobOfferRecord offer : offers) {
                if (deduplicationService.isDuplicate(contentBuilder.buildTechnicalContent(offer))) {
                    continue;
                }

                if (!matchingService.passesMustHave(offer)) {
                    continue;
                }

                double quickScore = matchingService.quickScore(offer);
                if (quickScore < properties.matching().aiThresholdMin()) continue;

                JobOfferRecord analyzed;

                if (quickScore >= properties.matching().aiThresholdMax()) {
                    double priority = priorityService.calculate(
                            offer.withMatchingScore(quickScore)
                    );

                    MatchingBand band = classificationService.classify(priority);

                    analyzed = offer
                            .withMatchingScore(quickScore)
                            .withPriorityScore(priority)
                            .withStatus(classificationService.toStatus(band));
                } else {
                    analyzed = analysisFacade.analyze(offer);
                }

                if (analyzed.priorityScore() < 60) {
                    log.info("SKIPPED (LOW PRIORITY): {} | {}", analyzed.title(), analyzed.priorityScore());
                    continue;
                }

                MatchingBand band = classificationService.classify(analyzed.priorityScore());
                OfferStatus status = classificationService.toStatus(band);

                JobOfferRecord enriched = analyzed.withStatus(status);

                log.info("FINAL → PRIORITY: {} | MATCH: {} | TITLE: {} | BAND: {}",
                        enriched.priorityScore(),
                        enriched.matchingScore(),
                        enriched.title(),
                        band
                );

                store.upsert(enriched);
                if (band == MatchingBand.INSTANT) {
                    notifier.sendInstant(enriched);
                }
            }
            log.info("Provider={} pobrane={}", provider.sourceName(), offers.size());
        }
    }

    private JobOfferRecord enrichIdentity(JobOfferRecord offer) {
        String id = offer.id() == null || offer.id().isBlank() ? UUID.randomUUID().toString() : offer.id();
        String fingerprint = fingerprintFactory.createFrom(offer.title(), offer.companyName(), offer.location());

        return offer.withIdAndFingerprint(id, fingerprint)
                .withStatus(OfferStatus.NEW);
    }

    @Scheduled(cron = "${workdone.scheduling.digest-cron}", zone = "${workdone.scheduling.zone-id}")
    public void sendDailyDigest() {
        LocalDate today = LocalDate.now(ZoneId.of(properties.scheduling().zoneId()));
        List<JobOfferRecord> digestOffers = store.findForDigest(today).stream()
                .filter(offer -> classificationService.classify(offer.priorityScore()) == MatchingBand.DIGEST)
                .toList();
        notifier.sendDigest(digestOffers);
    }
}