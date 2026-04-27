package com.workdone.backend.orchestration;

import com.workdone.backend.analysis.*;
import com.workdone.backend.analysis.OfferAnalysisFacade.AnalysisSource;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class OfferIngestionOrchestrator {

    private final List<JobProvider> providers;
    private final OfferEnricher offerEnricher;
    private final OfferStore store;
    private final OfferProcessor offerProcessor;
    private final CandidateProfileService candidateProfileService;
    private final DiscordNotifier notifier;
    private final OfferEmbeddingService embeddingService;
    private final JobSearchParametersProvider searchParametersProvider;
    private final DynamicConfigService dynamicConfigService;
    private final AiExecutionPolicy aiPolicy;
    private final OfferClassificationService classificationService;

    private final AtomicBoolean isIngestionRunning = new AtomicBoolean(false);

    @Scheduled(cron = "${workdone.scheduling.ingestion-cron}", zone = "${workdone.scheduling.zone-id}")
    public void runIngestion() {
        if (!isIngestionRunning.compareAndSet(false, true)) return;

        IngestionMetrics metrics = new IngestionMetrics();
        try {
            log.info("🚀 [INGESTION] Startujemy nowy run...");
            aiPolicy.resetBudget();

            float[] candidateVector = candidateProfileService.getCandidateVector();
            List<SearchContext> contexts = searchParametersProvider.getContexts();
            List<JobOfferRecord> allNewOffers = new ArrayList<>();

            // KROK 1: Ingestia i Deduplikacja wstępna
            for (SearchContext context : contexts) {
                for (JobProvider provider : providers) {
                    try {
                        List<JobOfferRecord> raw = provider.fetchOffers(context);
                        metrics.totalFetched += raw.size();

                        List<JobOfferRecord> unique = raw.stream()
                                .map(offerEnricher::cleanAndEnrich)
                                .filter(offer -> {
                                    boolean exists = store.existsBySourceOrFingerprint(offer);
                                    if (exists) metrics.alreadyExists++;
                                    return !exists;
                                })
                                .toList();
                        allNewOffers.addAll(unique);
                    } catch (Exception e) {
                        log.error("❌ Provider {} error: {}", provider.sourceName(), e.getMessage());
                    }
                }
            }

            if (allNewOffers.isEmpty()) {
                log.info("🏁 [INGESTION] Koniec. Brak nowych ofert. {}", metrics);
                return;
            }

            // KROK 2: Faza 1 - Embedding i Szybki Scoring
            List<OfferProcessor.ProcessingResult> candidates = new ArrayList<>();
            List<float[]> vectors = embeddingService.embedOffers(allNewOffers);

            for (int i = 0; i < allNewOffers.size(); i++) {
                JobOfferRecord offer = allNewOffers.get(i);
                float[] vector = (vectors.size() > i) ? vectors.get(i) : null;

                var result = offerProcessor.preProcess(offer, candidateVector, vector);
                if (result.processed()) {
                    metrics.processedPreAI++;
                    candidates.add(result);
                } else {
                    metrics.failedMustHave++;
                }
            }

            // KROK 3: Faza 2 - Sortowanie i Selekcja TOP-N do AI
            List<OfferProcessor.ProcessingResult> finalResults = candidates.stream()
                    .sorted(Comparator.comparing((OfferProcessor.ProcessingResult r) -> r.offer().matchingScore()).reversed())
                    .map(res -> {
                        var enriched = offerProcessor.enrichWithAi(res.offer(), candidateVector, res.vector());

                        // Zliczanie statystyk pod "Lejek"
                        if (enriched.source() == AnalysisSource.AI) metrics.aiCalls++;
                        else if (enriched.source() == AnalysisSource.CACHE) metrics.aiCacheHits++;
                        else if (enriched.source() == AnalysisSource.SKIPPED) metrics.aiSkips++;

                        if (enriched.band() == MatchingBand.INSTANT) metrics.instantFound++;

                        return enriched;
                    })
                    .toList();

            // KROK 4: Fallback na najlepszą ofertę (jeśli nie było INSTANT)
            JobOfferRecord best = finalResults.stream()
                    .map(OfferProcessor.ProcessingResult::offer)
                    .max(Comparator.comparing(JobOfferRecord::priorityScore))
                    .orElse(null);

            sendBestOfferFallbackIfNeeded(metrics.instantFound, best);

            log.info("🏁 [INGESTION] Run zakończony. {}", metrics);

        } finally {
            isIngestionRunning.set(false);
        }
    }

    @Scheduled(cron = "${workdone.scheduling.digest-cron}", zone = "${workdone.scheduling.zone-id}")
    public void sendDailyDigest() {
        log.info("📊 Przygotowuję Daily Digest...");

        // 1. Pobieramy oferty "pobłogosławione" przez AI (Status: ANALYZED, Band: DIGEST)
        List<JobOfferRecord> aiBlessed = store.findByStatus(OfferStatus.ANALYZED).stream()
                .filter(offer -> classificationService.classify(offer.priorityScore()) == MatchingBand.DIGEST)
                .toList();

        // 2. Pobieramy Top 10 "najlepszych z reszty" (Status: NEW, posortowane po semantyce)
        List<JobOfferRecord> bestOfRest = store.findByStatus(OfferStatus.NEW).stream()
                .sorted(Comparator.comparing(JobOfferRecord::matchingScore).reversed())
                .limit(10) // Limitujemy, żeby nie spamować
                .toList();

        // 3. Łączymy listy (AI Blessed mają priorytet na górze)
        List<JobOfferRecord> allDigestOffers = new ArrayList<>();
        allDigestOffers.addAll(aiBlessed);
        allDigestOffers.addAll(bestOfRest);

        if (!allDigestOffers.isEmpty()) {
            notifier.sendDigest(allDigestOffers);
            log.info("📊 Wysłano digest: {} (AI: {}, Raw: {})",
                    allDigestOffers.size(), aiBlessed.size(), bestOfRest.size());

            markAsSent(allDigestOffers);
        } else {
            log.info("📊 Brak nowych ofert do wysłania w digest.");
        }
    }

    private void markAsSent(List<JobOfferRecord> offers) {
        for (JobOfferRecord offer : offers) {
            store.updateStatusBySourceUrl(offer.sourceUrl(), OfferStatus.SENT);
        }
    }

    private void sendBestOfferFallbackIfNeeded(int instantOffersSent, JobOfferRecord bestOfferThisRun) {
        if (!dynamicConfigService.isBestOfferFallbackEnabled() || instantOffersSent > 0 || bestOfferThisRun == null) {
            return;
        }
        notifier.sendInstant(bestOfferThisRun);
        log.info("🏆 Wysłano najlepszą ofertę jako fallback po skanie: {} ({})",
                bestOfferThisRun.title(), String.format("%.1f", bestOfferThisRun.priorityScore()));
    }
}