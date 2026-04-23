package com.workdone.backend.orchestration;

import com.workdone.backend.analysis.MatchingBand;
import com.workdone.backend.analysis.OfferClassificationService;
import com.workdone.backend.analysis.OfferEmbeddingService;
import com.workdone.backend.analysis.DynamicConfigService;
import com.workdone.backend.config.WorkDoneProperties;
import com.workdone.backend.ingestion.JobProvider;
import com.workdone.backend.ingestion.JobSearchParametersProvider;
import com.workdone.backend.ingestion.SearchContext;
import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.model.OfferStatus;
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
    private final JobSearchParametersProvider searchParametersProvider;
    private final DynamicConfigService dynamicConfigService;

    private final AtomicBoolean isIngestionRunning = new AtomicBoolean(false);

    @Scheduled(cron = "${workdone.scheduling.ingestion-cron}", zone = "${workdone.scheduling.zone-id}")
    public void runIngestion() {
        if (!isIngestionRunning.compareAndSet(false, true)) {
            log.warn("⚠️ [INGESTION] Proces już trwa.");
            return;
        }

        int totalFound = 0;
        int totalNew = 0;
        int instantOffersSent = 0;
        JobOfferRecord bestOfferThisRun = null;

        try {
            log.info("🚀 [INGESTION] Startujemy...");
            
            float[] candidateVector = candidateProfileService.getCandidateVector();
            if (candidateVector == null) {
                log.error("❌ Profil kandydata jest pusty!");
                return;
            }

            List<SearchContext> contexts = searchParametersProvider.getContexts();
            log.info("📊 Mamy {} kontekstów wyszukiwania do sprawdzenia.", contexts.size());

            for (SearchContext context : contexts) {
                log.info("📍 Szukamy ofert dla: {} (Remote: {})", context.location(), context.remoteOnly());
                
                for (JobProvider provider : providers) {
                    try {
                        log.info("🔍 Provider: {} [Lokalizacja: {}]", provider.sourceName(), context.location());
                        List<JobOfferRecord> rawOffers = provider.fetchOffers(context);
                        totalFound += rawOffers.size();

                        List<JobOfferRecord> newOffers = rawOffers.stream()
                                .map(offerEnricher::cleanAndEnrich)
                                .collect(Collectors.toMap(JobOfferRecord::fingerprint, o -> o, (o1, o2) -> o1, LinkedHashMap::new))
                                .values().stream()
                                .filter(offer -> !store.existsBySourceOrFingerprint(offer))
                                .toList();

                        if (newOffers.isEmpty()) continue;

                        log.info("📥 Przetwarzam {} nowych ofert z {}...", newOffers.size(), provider.sourceName());
                        
                        List<float[]> offerVectors = new ArrayList<>();
                        try {
                            offerVectors = embeddingService.embedOffers(newOffers);
                        } catch (Exception e) {
                            log.error("❌ Błąd batch embeddingu: {}. Procesuję bez wektorów.", e.getMessage());
                        }

                        for (int i = 0; i < newOffers.size(); i++) {
                            JobOfferRecord offer = newOffers.get(i);
                            float[] vector = (offerVectors.size() > i) ? offerVectors.get(i) : null;
                            
                            try {
                                OfferProcessor.ProcessingResult result = offerProcessor.processOffer(offer, candidateVector, vector);
                                if (!result.processed()) {
                                    continue;
                                }
                                totalNew++;
                                if (result.band() == MatchingBand.INSTANT) {
                                    instantOffersSent++;
                                }
                                if (bestOfferThisRun == null || result.offer().priorityScore() > bestOfferThisRun.priorityScore()) {
                                    bestOfferThisRun = result.offer();
                                }
                                totalNew++;
                            } catch (ObjectOptimisticLockingFailureException e) {
                                log.warn("🔄 Optymistyczna blokada dla: {}", offer.title());
                            } catch (Exception e) {
                                log.error("❌ Błąd przy procesowaniu {}: {}", offer.title(), e.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        log.error("❌ Błąd dostawcy {} dla lokalizacji {}: {}", provider.sourceName(), context.location(), e.getMessage());
                    }
                }
            }
            sendBestOfferFallbackIfNeeded(instantOffersSent, bestOfferThisRun);
            log.info("🏁 [INGESTION] Koniec. Znaleziono łącznie: {}, Przetworzono nowych: {}", totalFound, totalNew);
        } finally {
            isIngestionRunning.set(false);
        }
    }

    private void sendBestOfferFallbackIfNeeded(int instantOffersSent, JobOfferRecord bestOfferThisRun) {
        if (!dynamicConfigService.isBestOfferFallbackEnabled()) {
            return;
        }
        if (instantOffersSent > 0) {
            return;
        }
        if (bestOfferThisRun == null) {
            log.info("ℹ️ Best-offer fallback włączony, ale brak ofert do wysłania po tym skanie.");
            return;
        }

        notifier.sendInstant(bestOfferThisRun);
        log.info("🏆 Wysłano najlepszą ofertę jako fallback po skanie: {} ({})",
                bestOfferThisRun.title(),
                String.format("%.1f", bestOfferThisRun.priorityScore()));
    }

    /**
     * Wysyła codzienne podsumowanie ofert zaklasyfikowanych jako DIGEST, 
     * które nadal czekają na Twoją decyzję.
     */
    @Scheduled(cron = "${workdone.scheduling.digest-cron}", zone = "${workdone.scheduling.zone-id}")
    public void sendDailyDigest() {
        LocalDate today = LocalDate.now(ZoneId.of(properties.scheduling().zoneId()));
        
        // Pobieramy oferty o statusie ANALYZED (czekające na decyzję)
        List<JobOfferRecord> digestOffers = store.findByStatus(OfferStatus.ANALYZED).stream()
                // Filtrujemy tylko te, które wpadają w pasmo DIGEST (czyli są "całkiem niezłe", ale nie "instant")
                .filter(offer -> classificationService.classify(offer.priorityScore()) == MatchingBand.DIGEST)
                .toList();
        
        if (!digestOffers.isEmpty()) {
            notifier.sendDigest(digestOffers);
            log.info("📊 Wysłano digest z {} ofertami.", digestOffers.size());
        } else {
            log.info("📊 Brak nowych ofert do wysłania w digest.");
        }
    }
}
