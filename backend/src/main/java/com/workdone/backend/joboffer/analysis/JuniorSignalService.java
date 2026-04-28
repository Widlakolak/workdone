package com.workdone.backend.joboffer.analysis;

import com.workdone.backend.common.model.JobOfferRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Slf4j
@Service
public class JuniorSignalService {

    public boolean isJuniorFriendly(JobOfferRecord offer) {
        String text = (offer.title() + " " + offer.rawDescription())
                .toLowerCase(Locale.ROOT);

        // Szukam słów kluczy, które sugerują, że to oferta na mój poziom (Junior/Intern)
        boolean positive =
                text.contains("junior") ||
                        text.contains("entry") ||
                        text.contains("trainee") ||
                        text.contains("intern") ||
                        text.contains("0-2"); // np. 0-2 years of experience

        // Wykluczam te, które wprost krzyczą "Senior" albo "Lead"
        boolean negative =
                text.contains("senior") ||
                        text.contains("lead") ||
                        text.contains("architect") ||
                        text.contains("5+"); // np. 5+ years

        // Oferta jest przyjazna, jeśli ma "pozytywne" sygnały i brak "negatywnych"
        boolean result = positive && !negative;
        if (result) {
            log.debug("Offer identified as Junior Friendly: {}", offer.title());
        }
        return result;
    }
}
