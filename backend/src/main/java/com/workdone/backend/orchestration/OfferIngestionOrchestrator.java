package com.workdone.backend.orchestration;

import com.workdone.backend.analysis.MatchingBand;
import com.workdone.backend.analysis.OfferClassificationService;
import com.workdone.backend.analysis.OfferEmbeddingService;
import com.workdone.backend.config.WorkDoneProperties;
import com.workdone.backend.ingestion.JobProvider;
import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.notification.DiscordNotifier;
import com.workdone.backend.profile.service.CandidateProfileService;
import com.workdone.backend.storage.OfferStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OfferIngestionOrchestrator {

    private final List<JobProvider> providers;
    private final OfferEnricher offerEnricher;
    private final OfferStore store;
    private final OfferProcessor offerProcessor;
    private final CandidateProfileService candidateProfileService;
    private final OfferClassificationService classificationService;
    private final DiscordNotifier notifier;
    private final WorkDoneProperties properties;
    private final OfferEmbeddingService embeddingService;

    private final AtomicBoolean isIngestionRunning = new AtomicBoolean(false);

    @Scheduled(cron = "${workdone.scheduling.ingestion-cron}", zone = "${workdone.scheduling.zone-id}")
    public void runIngestion() {
        if (!isIngestionRunning.compareAndSet(false, true)) {
            log.warn("⚠️ [INGESTION] Proces już trwa.");
            return;
        }

        int totalFound = 0;
        int totalNew = 0;

        try {
            log.info("🚀 [INGESTION] Startujemy...");
            
            float[] candidateVector = candidateProfileService.getCandidateVector();
            if (candidateVector == null) {
                log.error("❌ Profil kandydata jest pusty!");
                return;
            }

            for (JobProvider provider : providers) {
                try {
                    log.info("🔍 --- Provider: {} ---", provider.sourceName());
                    List<JobOfferRecord> rawOffers = provider.fetchOffers();
                    totalFound += rawOffers.size();

                    List<JobOfferRecord> newOffers = rawOffers.stream()
                            .map(offerEnricher::cleanAndEnrich)
                            .collect(Collectors.toMap(JobOfferRecord::fingerprint, o -> o, (o1, o2) -> o1, LinkedHashMap::new))
                            .values().stream()
                            .filter(offer -> !store.existsBySourceOrFingerprint(offer))
                            .toList();

                    if (newOffers.isEmpty()) continue;

                    log.info("📥 Przetwarzam {} nowych ofert z {} (Batch Embedding)...", newOffers.size(), provider.sourceName());
                    
                    // Batching embeddingów - to oszczędza RPM w Cohere!
                    List<float[]> offerVectors = new ArrayList<>();
                    try {
                        offerVectors = embeddingService.embedOffers(newOffers);
                    } catch (Exception e) {
                        log.error("❌ Błąd batch embeddingu dla {}: {}. Próbuję procesować pojedynczo.", provider.sourceName(), e.getMessage());
                    }

                    for (int i = 0; i < newOffers.size(); i++) {
                        JobOfferRecord offer = newOffers.get(i);
                        float[] vector = (offerVectors.size() > i) ? offerVectors.get(i) : null;
                        
                        try {
                            offerProcessor.processOffer(offer, candidateVector, vector);
                            totalNew++;
                        } catch (ObjectOptimisticLockingFailureException e) {
                            log.warn("🔄 Optymistyczna blokada dla: {}", offer.title());
                        } catch (Exception e) {
                            log.error("❌ Błąd przy procesowaniu {}: {}", offer.title(), e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.error("❌ Błąd dostawcy {}: {}", provider.sourceName(), e.getMessage());
                }
            }
            log.info("🏁 [INGESTION] Koniec. Znaleziono: {}, Przetworzono: {}", totalFound, totalNew);
        } finally {
            isIngestionRunning.set(false);
        }
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
