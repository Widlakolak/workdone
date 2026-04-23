package com.workdone.backend.interaction;

import com.workdone.backend.profile.service.CandidateProfileService;
import com.workdone.backend.notification.DiscordNotifier;
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
public class AdminController {

    private final OfferIngestionOrchestrator orchestrator;
    private final CandidateProfileService profileService;
    private final DiscordNotifier discordNotifier;

    @PostMapping("/run-ingestion")
    public String runIngestion() {
        log.info("🛠 Odpalam pobieranie ofert ręcznie z API.");
        Thread.ofVirtual().start(orchestrator::runIngestion);
        return "🚀 Maszyna ruszyła! Zaglądaj w logi.";
    }

    @PostMapping("/refresh-profile")
    public String refreshProfile() {
        log.info("🛠 Odświeżam moje dane z CV ręcznie z API.");
        boolean refreshed = profileService.refreshProfile();
        if (!refreshed) {
            return "❌ Nie udało się odświeżyć profilu. Sprawdź logi backendu (np. quota/API).";
        }
        return "✅ Skille i seniority odświeżone! Teraz jestem: " + profileService.getSeniority();
    }

    @PostMapping("/show-panel")
    public String showPanel() {
        log.info("🎮 Wysyłam Master Control Panel na Discorda.");
        discordNotifier.sendControlPanel();
        return "🎮 Panel wysłany na Discorda!";
    }

}
