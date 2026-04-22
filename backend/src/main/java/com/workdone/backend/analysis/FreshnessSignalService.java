package com.workdone.backend.analysis;

import com.workdone.backend.model.JobOfferRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
public class FreshnessSignalService {

    public double calculatePenalty(JobOfferRecord offer) {
        if (offer.publishedAt() == null) {
            log.trace("Brak daty publikacji dla {}, pomijam karę.", offer.title());
            return 0;
        }

        long hours = Duration.between(offer.publishedAt(), LocalDateTime.now()).toHours();
        // Za każdą godzinę od publikacji odejmuję 0.1 pkt - im starsza oferta, tym mniejsza szansa na sukces
        double penalty = hours * 0.1;
        
        // Nie chcę jednak, żeby wiek oferty całkowicie ją zabił, więc limituję karę do 15 pkt
        double finalPenalty = Math.min(penalty, 15);

        if (finalPenalty > 0) {
            log.debug("Kara za świeżość dla {} ({}h temu): {}", offer.title(), hours, finalPenalty);
        }
        return finalPenalty;
    }
}
