package com.workdone.backend.analysis;

import com.workdone.backend.model.JobOfferRecord;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class LocationSignalService {

    public boolean isRemote(JobOfferRecord offer) {
        String text = buildText(offer);
        return text.contains("remote");
    }

    public boolean isHybrid(JobOfferRecord offer) {
        String text = buildText(offer);
        return text.contains("hybrid");
    }

    public boolean isPoland(JobOfferRecord offer) {
        String text = buildText(offer);
        return text.contains("poland") || text.contains("polska");
    }

    private String buildText(JobOfferRecord offer) {
        return (offer.location() + " " + offer.rawDescription())
                .toLowerCase(Locale.ROOT);
    }

    public boolean isRemoteFromLocation(JobOfferRecord offer) {
        return offer.location().toLowerCase().contains("remote");
    }

    public boolean isAllowedLocation(JobOfferRecord offer) {
        String loc = offer.location().toLowerCase();

        if (loc.contains("remote") || loc.contains("europe") || loc.contains("world")) {
            return true;
        }

        if (loc.contains("poland") || loc.contains("polska")) {
            return true;
        }

        return isRemote(offer) || isHybrid(offer);
    }
}