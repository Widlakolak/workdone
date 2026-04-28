package com.workdone.backend.joboffer.orchestration;

import com.workdone.backend.joboffer.analysis.*;
import com.workdone.backend.joboffer.analysis.OfferAnalysisFacade.AnalysisSource;
import com.workdone.backend.joboffer.ingestion.JobProvider;
import com.workdone.backend.joboffer.ingestion.JobSearchParametersProvider;
import com.workdone.backend.joboffer.ingestion.SearchContext;
import com.workdone.backend.common.model.JobOfferRecord;
import com.workdone.backend.common.model.OfferStatus;
import com.workdone.backend.joboffer.notification.DiscordNotifier;
import com.workdone.backend.profile.service.CandidateProfileService;
import com.workdone.backend.joboffer.storage.OfferStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final OfferDeduplicationService offerDeduplicationService;

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
            Map<String, List<JobOfferRecord>> fetchCache = new HashMap<>();
            Set<String> seenInRun = new HashSet<>();

            // KROK 1: Ingestia i Deduplikacja wstępna
            List<JobProvider> globalProviders = providers.stream()
                    .filter(p -> p.scope() == JobProvider.Scope.GLOBAL)
                    .toList();
            List<JobProvider> contextualProviders = providers.stream()
                    .filter(p -> p.scope() == JobProvider.Scope.CONTEXTUAL)
                    .toList();

            SearchContext fallbackContext = contexts.isEmpty() ? SearchContext.builder()
                    .keywords(List.of("java"))
                    .location(SearchContext.REMOTE_GLOBAL)
                    .remoteOnly(true)
                    .maxResults(100)
                    .build() : contexts.getFirst();

            for (JobProvider provider : globalProviders) {
                fetchAndCollect(provider, fallbackContext, fetchCache, seenInRun, allNewOffers, metrics);
            }

            for (SearchContext context : contexts) {
                for (JobProvider provider : contextualProviders) {
                    fetchAndCollect(provider, context, fetchCache, seenInRun, allNewOffers, metrics);
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
                if (offerDeduplicationService.isDuplicate(vector)) {
                    metrics.alreadyExists++;
                    continue;
                }

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

    private void fetchAndCollect(JobProvider provider,
                                 SearchContext context,
                                 Map<String, List<JobOfferRecord>> fetchCache,
                                 Set<String> seenInRun,
                                 List<JobOfferRecord> allNewOffers,
                                 IngestionMetrics metrics) {
        String requestKey = provider.requestKey(context);
        List<JobOfferRecord> raw;
        if (fetchCache.containsKey(requestKey)) {
            raw = fetchCache.get(requestKey);
            log.debug("♻️ [CACHE HIT] {} -> {}", provider.sourceName(), requestKey);
        } else {
            try {
                raw = provider.fetchOffers(context);
                fetchCache.put(requestKey, raw);
            } catch (Exception e) {
                log.error("❌ Provider {} error: {}", provider.sourceName(), e.getMessage());
                return;
            }
        }

        metrics.totalFetched += raw.size();

        List<JobOfferRecord> unique = raw.stream()
                .map(offerEnricher::cleanAndEnrich)
                .filter(offer -> {
                    boolean seen = seenInRun.contains(offer.sourceUrl()) || seenInRun.contains(offer.fingerprint());
                    if (seen) {
                        metrics.alreadyExists++;
                        return false;
                    }

                    boolean exists = store.existsBySourceOrFingerprint(offer);
                    if (exists) {
                        metrics.alreadyExists++;
                        return false;
                    }

                    seenInRun.add(offer.sourceUrl());
                    seenInRun.add(offer.fingerprint());
                    return true;
                })
                .toList();

        allNewOffers.addAll(unique);
    }
}