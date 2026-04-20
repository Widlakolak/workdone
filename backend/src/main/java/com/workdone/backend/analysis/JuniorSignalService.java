package com.workdone.backend.analysis;

import com.workdone.backend.model.JobOfferRecord;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class JuniorSignalService {

    public boolean isJuniorFriendly(JobOfferRecord offer) {
        String text = (offer.title() + " " + offer.rawDescription())
                .toLowerCase(Locale.ROOT);

        boolean positive =
                text.contains("junior") ||
                        text.contains("entry") ||
                        text.contains("trainee") ||
                        text.contains("intern") ||
                        text.contains("0-2");

        boolean negative =
                text.contains("senior") ||
                        text.contains("lead") ||
                        text.contains("architect") ||
                        text.contains("5+");

        return positive && !negative;
    }
}