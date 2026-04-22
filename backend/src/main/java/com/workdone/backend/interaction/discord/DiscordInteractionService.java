package com.workdone.backend.interaction.discord;

import com.workdone.backend.profile.service.CandidateProfileService;
import com.workdone.backend.analysis.DynamicConfigService;
import com.workdone.backend.model.OfferStatus;
import com.workdone.backend.orchestration.OfferIngestionOrchestrator;
import com.workdone.backend.storage.OfferStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Serwis obsługujący to, co kliknę w Discordzie. 
 * Jak kliknę "Aplikowano", "Odrzuć" albo przycisk w panelu sterowania - tutaj trafia logika.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscordInteractionService {

    private final OfferStore store;
    private final DynamicConfigService dynamicConfig;
    private final CandidateProfileService profileService;
    private final OfferIngestionOrchestrator orchestrator;

    public String handleCustomId(String customId) {
        log.info("📥 Obsługuję kliknięcie z Discorda: {}", customId);

        // Obsługa mojego panelu sterowania (zaczyna się od 'config|')
        if (customId.startsWith("config|")) {
            String[] parts = customId.split("\\|");
            
            // Proste akcje bez parametrów (np. config|status)
            if (parts.length == 2) {
                switch (parts[1]) {
                    case "status" -> { return dynamicConfig.getCurrentStatus(); }
                    case "refresh_cv" -> {
                        profileService.refreshProfile();
                        return "✅ CV przeanalizowane ponownie. Seniority: " + profileService.getSeniority();
                    }
                    case "use_cv_skills" -> {
                        var skills = String.join(",", profileService.getSuggestedKeywords());
                        dynamicConfig.updateConfig("musthave", skills);
                        return "✅ Aktywowano słowa kluczowe z CV jako Must-Have: " + skills;
                    }
                    case "run_ingestion" -> {
                        log.info("🛠 Odpalam szukanie ofert na prośbę z Discorda.");
                        Thread.ofVirtual().start(orchestrator::runIngestion);
                        return "🚀 Proces szukania ruszył! Czekaj na powiadomienia.";
                    }
                }
            }
            
            // Akcje z wartością (np. config|semantic|80)
            if (parts.length == 3) {
                return dynamicConfig.updateConfig(parts[1], parts[2]);
            }
        }

        // Obsługa akcji na konkretnej ofercie (Applied / Reject)
        ParsedAction action = parse(customId);
        if (action == null) return "❌ Coś nie tak z tą akcją (błąd parsowania).";

        OfferStatus newStatus = switch (action.action()) {
            case "applied" -> OfferStatus.APPLIED;
            case "reject" -> OfferStatus.REJECTED;
            default -> null;
        };

        if (newStatus == null) return "❌ Nie wiem, co mam zrobić z tą ofertą.";

        // Aktualizuję status w bazie, żeby wiedzieć na co już aplikowałem
        boolean updated = store.updateStatusBySourceUrl(action.sourceUrl(), newStatus);
        return updated ? "✅ Zapisałem wybór: " + action.action() + "." : "❌ Nie znalazłem tej oferty w bazie (może stara?).";
    }

    private ParsedAction parse(String customId) {
        // customId ma format "akcja|url", np. "applied|https://job.com/123"
        String[] parts = customId.split("\\|", 2);
        return parts.length == 2 ? new ParsedAction(parts[0].toLowerCase(Locale.ROOT), parts[1]) : null;
    }

    private record ParsedAction(String action, String sourceUrl) {}
}
