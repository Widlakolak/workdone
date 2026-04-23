package com.workdone.backend.profile.service;

import com.workdone.backend.analysis.DynamicConfigService;
import com.workdone.backend.analysis.OfferEmbeddingService;
import com.workdone.backend.profile.parser.CvSemanticParser;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Serwis zarządzający moim profilem. Trzyma wektor CV (do porównań), 
 * wyciągnięte skille, seniority i moją lokalizację.
 */
@Slf4j
@Service
public class CandidateProfileService {

    private final CvAggregationService cvAggregationService;
    private final OfferEmbeddingService embeddingService;
    private final CvSemanticParser cvSemanticParser;
    private final Environment environment;
    private final DynamicConfigService dynamicConfigService;

    // To jest "esencja" mojego CV w formie liczbowej, której używam do szukania dopasowań
    private float[] candidateVector;
    
    @Getter
    private String latestProfileText;

    @Getter
    private List<String> suggestedKeywords = new ArrayList<>();

    @Getter
    private String seniority;

    @Getter
    private String location;

    public CandidateProfileService(CvAggregationService cvAggregationService, 
                                   OfferEmbeddingService embeddingService, 
                                   CvSemanticParser cvSemanticParser, 
                                   Environment environment, 
                                   @Lazy DynamicConfigService dynamicConfigService) {
        this.cvAggregationService = cvAggregationService;
        this.embeddingService = embeddingService;
        this.cvSemanticParser = cvSemanticParser;
        this.environment = environment;
        this.dynamicConfigService = dynamicConfigService;
    }

    @PostConstruct
    public void init() {
        // W testach nie chcę strzelać do AI, więc podstawiam pusty wektor
        if (isTestProfileActive()) {
            log.info("🧪 [TEST] Profil kandydata - pusty wektor.");
            this.candidateVector = new float[1024];
            this.latestProfileText = "Test profile content";
            this.seniority = "junior";
            this.location = "Poland";
            return;
        }
        
        // Na start aplikacji odświeżam profil w osobnym wątku, żeby nie mulić startu Springa
        log.info("🚀 Inicjalne odświeżanie profilu CV...");
        CompletableFuture.runAsync(() -> refreshProfile())
                .exceptionally(ex -> {
                    log.error("❌ Błąd inicjalizacji profilu: {}", ex.getMessage());
                    return null;
                });
    }

    /**
     * Główna metoda odświeżająca dane o mnie. 
     * Łączy pliki CV, tworzy wektor i wyciąga cechy przez AI.
     */
    public synchronized boolean refreshProfile() {
        log.info("🔄 Odświeżanie profilu kandydata...");
        try {
            // 1. Zbieram tekst ze wszystkich PDF-ów w folderze
            String profileText = cvAggregationService.buildMergedProfile();
            if (profileText == null || profileText.isBlank()) {
                log.warn("⚠️ Brak dokumentów profilu!");
                return false;
            }

            this.latestProfileText = profileText;
            
            // 2. Generuję wektor (embedding) dla całego mojego profilu
            this.candidateVector = embeddingService.embed(profileText);

            // 3. Pytam AI o szczegóły (tagi, lata expa, lokalizacja)
            try {
                CvSemanticParser.CvProfileResult result = cvSemanticParser.parse(profileText);
                if (result != null) {
                    this.suggestedKeywords = result.topKeywords();
                    this.seniority = result.seniority();
                    this.location = result.location();

                    // Synchronizacja lokalizacji z ogólną konfiguracją wyszukiwania
                    dynamicConfigService.syncWithProfile();
                } else {
                    log.warn("⚠️ Parser CV zwrócił pusty wynik. Zostawiam poprzednie metadane profilu.");
                }
            } catch (Exception parserException) {
                log.warn("⚠️ Nie udało się odświeżyć metadanych CV ({}). Zostawiam poprzednie metadane, ale wektor profilu jest dostępny.",
                        parserException.getMessage());
            }

            log.info("✅ Profil odświeżony. Seniority: {}, Location: {}, Keywords: {}", seniority, location, suggestedKeywords);
            return true;
        } catch (Exception e) {
            log.error("❌ Błąd odświeżania profilu: {}", e.getMessage());
            return false;
        }
    }

    public float[] getCandidateVector() {
        if (candidateVector == null) {
            log.warn("⚠️ Wektor kandydata pusty - próbuję odświeżyć 'na leniwca'.");
            refreshProfile();
        }
        return candidateVector;
    }

    private boolean isTestProfileActive() {
        return Arrays.asList(environment.getActiveProfiles()).contains("test");
    }
}
