package com.workdone.backend.interaction.discord;

import com.workdone.backend.model.OfferStatus;
import com.workdone.backend.storage.InMemoryOfferStore;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class DiscordInteractionService {

    private final InMemoryOfferStore store;

    public DiscordInteractionService(InMemoryOfferStore store) {
        this.store = store;
    }

    public String handleCustomId(String customId) {
        ParsedAction action = parse(customId);
        if (action == null) {
            return "Nie udało się rozpoznać akcji.";
        }

        OfferStatus newStatus = switch (action.action()) {
            case "applied" -> OfferStatus.APPLIED;
            case "reject" -> OfferStatus.REJECTED;
            default -> null;
        };

        if (newStatus == null) {
            return "Nieobsługiwana akcja: " + action.action();
        }

        boolean updated = store.updateStatusBySourceUrl(action.sourceUrl(), newStatus);
        if (!updated) {
            return "Nie znaleziono oferty dla URL: " + action.sourceUrl();
        }

        return "✅ Status oferty zaktualizowany na: " + newStatus;
    }

    private ParsedAction parse(String customId) {
        if (customId == null || customId.isBlank()) {
            return null;
        }

        String[] parts = customId.split("\\|", 2);
        if (parts.length != 2) {
            return null;
        }

        return new ParsedAction(parts[0].toLowerCase(Locale.ROOT), parts[1]);
    }

    private record ParsedAction(String action, String sourceUrl) {
    }
}