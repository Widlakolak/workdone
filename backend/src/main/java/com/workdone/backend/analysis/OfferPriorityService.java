package com.workdone.backend.analysis;

import com.workdone.backend.model.JobOfferRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferPriorityService {

    private final JuniorSignalService juniorSignalService;
    private final LocationSignalService locationSignalService;
    private final FreshnessSignalService freshnessSignalService;
    private final OfferMatchingService offerMatchingService;

    public double calculate(JobOfferRecord offer) {
        if (offer.matchingScore() == null) {
            log.warn("Matching score is null for offer: {}. Returning 0.0 priority.", offer.title());
            return 0.0;
        }

        double base = offer.matchingScore();
        double boost = 0.0;

        // Szukamy ofert przyjaznych Juniorom - to priorytet
        if (juniorSignalService.isJuniorFriendly(offer)) {
            boost += 20;
            log.debug("Junior friendly boost: +20");
        }

        // Punkty za lokalizację - im bliżej lub bardziej zdalnie, tym lepiej
        if (locationSignalService.isHybrid(offer)) {
            boost += 10;
            log.debug("Hybrid location boost: +10");
        }

        if (locationSignalService.isRemote(offer)) {
            boost += 20;
            log.debug("Remote location boost: +20");
        }

        if (locationSignalService.isPoland(offer)) {
            boost += 15;
            log.debug("Poland location boost: +15");
        }

        // Kara za oferty z zagranicy, które nie są remote (nie pojadę tam do biura)
        if (!locationSignalService.isPoland(offer)
                && !locationSignalService.isRemoteFromLocation(offer)) {
            boost -= 10;
            log.debug("Non-Poland and non-remote location penalty: -10");
        }

        if (!locationSignalService.isAllowedLocation(offer)) {
            boost -= 15;
            log.debug("Disallowed location penalty: -15");
        }

        // Jak dopasowanie słabe, to boosty też tniemy o połowę
        if (base < 50) {
            boost *= 0.5;
            log.debug("Base score < 50, boost halved.");
        }

        // Dodatkowy bonus za trafienie w moje Must-Have
        if (offerMatchingService.passesMustHave(offer)) {
            boost += 10;
            log.debug("Passes Must-Have boost: +10");
        }

        double penalty = freshnessSignalService.calculatePenalty(offer);

        double rawResult = base + boost - penalty;

        log.info("""
                        OFFER ANALYSIS:
                        TITLE: {}
                        MATCH: {}
                        BOOST: {}
                        PENALTY: {}
                        FINAL: {}
                        LOCATION: {}
                        """,
                offer.title(),
                base,
                boost,
                penalty,
                rawResult,
                offer.location()
        );

        return clamp(rawResult, 0, 100);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
