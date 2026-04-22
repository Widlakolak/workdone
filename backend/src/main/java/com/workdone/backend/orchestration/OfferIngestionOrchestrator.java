package com.workdone.backend.orchestration;

import com.workdone.backend.analysis.MatchingBand;
import com.workdone.backend.analysis.OfferClassificationService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Wielki dyrygent całego procesu. Harmonogramuje pobieranie ofert i wysyłkę podsumowań. 
 * Pilnuje, żeby nie odpalić dwóch procesów pobierania naraz.
 */
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

    // Prosta blokada, żeby nie nałożyć na siebie dwóch cykli pobierania
    private final AtomicBoolean isIngestionRunning = new AtomicBoolean(false);

    /**
     * Główny cykl pobierania ofert. Odpala się zgodnie z cronem (np. co godzinę).
     */
    @Scheduled(cron = "${workdone.scheduling.ingestion-cron}", zone = "${workdone.scheduling.zone-id}")
    public void runIngestion() {
        if (!isIngestionRunning.compareAndSet(false, true)) {
            log.warn("⚠️ [INGESTION] Proces już trwa, nie odpalam kolejnego.");
            return;
        }

        int totalFound = 0;
        int totalNew = 0;

        try {
            log.info("🚀 [INGESTION] Startujemy z szukaniem ofert...");
            notifier.sendAiAlert("🚀 **Ingestion Cycle Started.** Searching for new opportunities...");

            // Bez mojego profilu wektorowego nie ma co szukać, bo nie mamy z czym porównywać
            float[] candidateVector = candidateProfileService.getCandidateVector();
            if (candidateVector == null) {
                log.error("❌ Mój profil jest pusty! Napraw to (wrzuć CV).");
                notifier.sendAiAlert("❌ **CRITICAL:** Candidate profile is empty! Please upload CV to enable matching.");
                return;
            }

            for (JobProvider provider : providers) {
                try {
                    log.info("🔍 --- Przeczesuję: {} ---", provider.sourceName());

                    List<JobOfferRecord> rawOffers = provider.fetchOffers();
                    int foundCount = rawOffers.size();
                    totalFound += foundCount;
                    log.info("📥 Dostałem {} surowych ofert od {}", foundCount, provider.sourceName());

                    // Czyścimy, nadajemy fingerprinty i wywalamy to, co już mam w bazie
                    List<JobOfferRecord> offersToProcess = rawOffers.stream()
                            .map(offerEnricher::cleanAndEnrich)
                            .collect(Collectors.toMap(
                                    JobOfferRecord::fingerprint,
                                    o -> o,
                                    (o1, o2) -> o1,
                                    LinkedHashMap::new))
                            .values().stream()
                            .filter(offer -> !store.existsBySourceOrFingerprint(offer))
                            .toList();
                    
                    int newCount = offersToProcess.size();
                    totalNew += newCount;
                    log.info("📥 Mam {} całkiem nowych, unikalnych ofert do analizy z {}", newCount, provider.sourceName());

                    for (JobOfferRecord offerRecord : offersToProcess) {
                        try {
                            offerProcessor.processOffer(offerRecord, candidateVector);
                        } catch (ObjectOptimisticLockingFailureException e) {
                            log.warn("🔄 Ktoś już edytował tę ofertę ({}), odpuszczam.", offerRecord.title());
                        } catch (Exception e) {
                            log.error("❌ Błąd przy procesowaniu {}: {}", offerRecord.title(), e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.error("❌ Błąd dostawcy {}: {}", provider.sourceName(), e.getMessage());
                    notifier.sendAiAlert("⚠️ Provider **" + provider.sourceName() + "** failed: " + e.getMessage());
                }
            }
            log.info("🏁 [INGESTION] Koniec cyklu. Łącznie znaleziono: {}, Nowych: {}", totalFound, totalNew);
            notifier.sendAiAlert("🏁 **Ingestion Cycle Finished.** Total found: " + totalFound + ", New analyzed: " + totalNew);
        } finally {
            isIngestionRunning.set(false);
        }
    }

    /**
     * Generuje i wysyła codzienne podsumowanie ofert "Dobre, ale nie pilne" (DIGEST).
     */
    @Scheduled(cron = "${workdone.scheduling.digest-cron}", zone = "${workdone.scheduling.zone-id}")
    public void sendDailyDigest() {
        log.info("📊 Szykuję raport dzienny...");
        LocalDate today = LocalDate.now(ZoneId.of(properties.scheduling().zoneId()));
        
        // Wybieram tylko te oferty z dzisiaj, które wylądowały w kubełku DIGEST
        List<JobOfferRecord> digestOffers = store.findForDigest(today).stream()
                .filter(offer -> classificationService.classify(offer.priorityScore()) == MatchingBand.DIGEST)
                .toList();

        log.info("📊 Znaleziono {} ofert do raportu.", digestOffers.size());
        notifier.sendDigest(digestOffers);
    }
}
