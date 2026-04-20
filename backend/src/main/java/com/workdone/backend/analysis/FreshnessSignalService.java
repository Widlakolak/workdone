package com.workdone.backend.analysis;

import com.workdone.backend.model.JobOfferRecord;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class FreshnessSignalService {

    public double calculatePenalty(JobOfferRecord offer) {
        if (offer.publishedAt() == null) {
            return 0;
        }

        long hours = Duration.between(offer.publishedAt(), LocalDateTime.now()).toHours();

        double penalty = hours * 0.3;

        return Math.min(penalty, 40);
    }
}