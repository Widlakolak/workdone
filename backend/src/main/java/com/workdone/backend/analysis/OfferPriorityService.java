package com.workdone.backend.analysis;

import com.workdone.backend.model.JobOfferRecord;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OfferPriorityService {

    private final JuniorSignalService juniorSignalService;
    private final LocationSignalService locationSignalService;
    private final FreshnessSignalService freshnessSignalService;
    private final OfferMatchingService offerMatchingService;

    private static final Logger log = LoggerFactory.getLogger(OfferPriorityService.class);

    public OfferPriorityService(JuniorSignalService juniorSignalService,
                                LocationSignalService locationSignalService,
                                FreshnessSignalService freshnessSignalService, OfferMatchingService offerMatchingService) {
        this.juniorSignalService = juniorSignalService;
        this.locationSignalService = locationSignalService;
        this.freshnessSignalService = freshnessSignalService;
        this.offerMatchingService = offerMatchingService;
    }

    public double calculate(JobOfferRecord offer) {
        if (offer.matchingScore() == null) {
            return 0.0;
        }

        double base = offer.matchingScore();
        double boost = 0.0;

        // 🔥 JUNIOR
        if (juniorSignalService.isJuniorFriendly(offer)) {
            boost += 20;
        }

        // 🔥 LOCATION
        if (locationSignalService.isHybrid(offer)) {
            boost += 10;
        }

        if (locationSignalService.isRemote(offer)) {
            boost += 20;
        }

        if (locationSignalService.isPoland(offer)) {
            boost += 15;
        }

        if (!locationSignalService.isPoland(offer)
                && !locationSignalService.isRemoteFromLocation(offer)) {
            boost -= 10;
        }

        if (!locationSignalService.isAllowedLocation(offer)) {
            boost -= 15;
        }

        if (base < 50) {
            boost *= 0.5;
        }

        if (offerMatchingService.passesMustHave(offer)) {
            boost += 10;
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