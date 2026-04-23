package com.workdone.backend.interaction.discord;

import com.workdone.backend.profile.service.CandidateProfileService;
import com.workdone.backend.analysis.DynamicConfigService;
import com.workdone.backend.model.OfferStatus;
import com.workdone.backend.notification.DiscordNotifier;
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
    private final DiscordNotifier discordNotifier;

    public String handleCustomId(String customId) {
        log.info("📥 Obsługuję kliknięcie z Discorda: {}", customId);

        if (customId.startsWith("config|")) {
            String[] parts = customId.split("\\|");
            
            if (parts.length == 2) {
                switch (parts[1]) {
                    case "status" -> { return dynamicConfig.getCurrentStatus(); }
                    case "help" -> { return getHelpMessage(); }
                    case "pending" -> {
                        var pendingOffers = store.findByStatus(OfferStatus.ANALYZED);
                        if (pendingOffers.isEmpty()) return "✅ Brak ofert oczekujących na decyzję.";
                        
                        // Wysyłam je asynchronicznie, żeby nie blokować odpowiedzi do Discorda (timeout)
                        Thread.ofVirtual().start(() -> {
                            pendingOffers.forEach(discordNotifier::sendInstant);
                        });
                        return "📨 Wysyłam " + pendingOffers.size() + " ofert do decyzji...";
                    }
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
            
            if (parts.length == 3) {
                return dynamicConfig.updateConfig(parts[1], parts[2]);
            }
        }

        ParsedAction action = parse(customId);
        if (action == null) return "❌ Coś nie tak z tą akcją (błąd parsowania).";

        OfferStatus newStatus = switch (action.action()) {
            case "applied" -> OfferStatus.APPLIED;
            case "reject" -> OfferStatus.REJECTED;
            default -> null;
        };

        if (newStatus == null) return "❌ Nie wiem, co mam zrobić z tą ofertą.";

        boolean updated = store.updateStatusBySourceUrl(action.sourceUrl(), newStatus);
        return updated ? "✅ Zapisałem wybór: " + action.action() + "." : "❌ Nie znalazłem tej oferty w bazie (może stara?).";
    }

    private String getHelpMessage() {
        return """
                💡 **Dostępne polecenia (przez API/Interakcje):**
                
                🛠 **Panel Sterowania:**
                - `status` - aktualne filtry i miasta
                - `pending` - wyślij oferty czekające na decyzję
                - `refresh_cv` - ponowne AI skanowanie plików CV
                - `run_ingestion` - natychmiastowe szukanie ofert
                
                ⚙️ **Konfiguracja (Parametry):**
                - `semantic|0.x` - próg dopasowania AI
                - `seniority|level` - junior/mid/senior
                - `location|city:R:H:O:days` - dodaj miasto
                - `clear_locations` - usuń wszystkie miasta
                """;
    }

    private ParsedAction parse(String customId) {
        String[] parts = customId.split("\\|", 2);
        return parts.length == 2 ? new ParsedAction(parts[0].toLowerCase(Locale.ROOT), parts[1]) : null;
    }

    private record ParsedAction(String action, String sourceUrl) {}
}
