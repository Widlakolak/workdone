package com.workdone.backend.joboffer.analysis;

import com.workdone.backend.common.model.JobOfferRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Serwis sygnałów lokalizacji. Teraz korzysta z LocationGuard jako źródła prawdy.
 * Tu sprawdzam czy to nie duplikat logiki, żeby nie było bajzlu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationSignalService {

    private final LocationGuard locationGuard;

    public boolean isRemote(JobOfferRecord offer) {
        String fullText = (offer.location() + " " + offer.rawDescription()).toLowerCase(Locale.ROOT);
        return locationGuard.isRemote(fullText);
    }

    public boolean isHybrid(JobOfferRecord offer) {
        String fullText = (offer.location() + " " + offer.rawDescription()).toLowerCase(Locale.ROOT);
        return locationGuard.isHybrid(fullText);
    }

    public boolean isPoland(JobOfferRecord offer) {
        String text = (offer.location() + " " + offer.rawDescription()).toLowerCase(Locale.ROOT);
        return text.contains("poland") || text.contains("polska");
    }

    public boolean isRemoteFromLocation(JobOfferRecord offer) {
        return offer.location().toLowerCase(Locale.ROOT).contains("remote");
    }

    public boolean isAllowedLocation(JobOfferRecord offer) {
        // Korzystam z głównej logiki akceptacji
        return locationGuard.isAccepted(offer);
    }
}
