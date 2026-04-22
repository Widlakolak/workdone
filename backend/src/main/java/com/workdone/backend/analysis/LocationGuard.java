package com.workdone.backend.analysis;

import com.workdone.backend.model.JobOfferRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

/**
 * Strażnik lokalizacji. Decyduje, czy w ogóle chcemy patrzeć na tę ofertę.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationGuard {

    private final DynamicConfigService dynamicConfig;

    private static final Set<String> POLISH_CITIES = Set.of("warszawa", "warsaw", "lodz", "łódź", "krakow", "kraków", "wroclaw", "wrocław", "poznan", "poznań", "gdansk", "gdańsk");
    
    // Tu wpisuję miasta, do których realnie chce mi się dojeżdżać na hybrydę
    private static final Set<String> HYBRID_ALLOWED_CITIES = Set.of("lodz", "łódź", "warszawa", "warsaw");

    public boolean isAccepted(JobOfferRecord offer) {
        String location = normalize(offer.location());
        String description = normalize(offer.rawDescription());
        String fullText = location + " " + description;

        // Zdalna to zawsze strzał w dziesiątkę, bierzemy jak leci
        if (isRemote(offer)) {
            return true;
        }

        // Sprawdzam, czy miasto z profilu (preferredLocation) pasuje
        String preferred = normalize(dynamicConfig.getPreferredLocation());
        if (!preferred.isEmpty() && location.contains(preferred)) {
            return true;
        }

        // Przy hybrydzie sprawdzam, czy miasto mi pasuje logistycznie
        if (isHybrid(offer)) {
            return isCityAllowed(location, HYBRID_ALLOWED_CITIES);
        }

        // Stacjonarnie to w sumie tylko Łódź wchodzi w grę (blisko domu)
        if (location.contains("lodz") || location.contains("łódź")) {
            return true;
        }

        // Jak miasto jest w PL i nie wykluczyli pracy zdalnej wprost, to puszczam do AI
        return isCityAllowed(location, POLISH_CITIES);
    }

    public boolean isRemote(JobOfferRecord offer) {
        String text = normalize(offer.location() + " " + offer.rawDescription());
        // Szukam słów kluczy dla pracy zdalnej
        return text.contains("remote") || text.contains("anywhere") || text.contains("100% remote") || text.contains("zdalna");
    }

    public boolean isHybrid(JobOfferRecord offer) {
        String text = normalize(offer.location() + " " + offer.rawDescription());
        // Hybryda to hybryda, trzeba sprawdzić czy nie za daleko
        return text.contains("hybrid") || text.contains("hybrydowa");
    }

    private boolean isCityAllowed(String location, Set<String> allowedSet) {
        return allowedSet.stream().anyMatch(location::contains);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
