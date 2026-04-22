package com.workdone.backend.interaction;

import com.workdone.backend.profile.service.CandidateProfileService;
import com.workdone.backend.orchestration.OfferIngestionOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mój tajny panel administracyjny przez REST. 
 * Pozwala mi "kopnąć" bota, żeby zaczął szukać ofert albo przeładował moje CV bez czekania na crona.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/test")
@RequiredArgsConstructor
public class AdminTestController {

    private final OfferIngestionOrchestrator orchestrator;
    private final CandidateProfileService profileService;

    @PostMapping("/run-ingestion")
    public String runIngestion() {
        log.info("🛠 Odpalam pobieranie ofert ręcznie z API.");
        // Puszczam to w tle (virtual thread), żeby nie blokować mojego requesta (nie chcę timeoutu)
        Thread.ofVirtual().start(orchestrator::runIngestion);
        return "🚀 Maszyna ruszyła! Zaglądaj w logi.";
    }

    @PostMapping("/refresh-profile")
    public String refreshProfile() {
        log.info("🛠 Odświeżam moje dane z CV ręcznie z API.");
        profileService.refreshProfile();
        return "✅ Skille i seniority odświeżone! Teraz jestem: " + profileService.getSeniority();
    }
}
