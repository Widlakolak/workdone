package com.workdone.backend.analysis;

import com.workdone.backend.config.WorkDoneProperties;
import com.workdone.backend.model.OfferStatus;
import org.springframework.stereotype.Service;

@Service
public class OfferClassificationService {

    private final WorkDoneProperties properties;

    public OfferClassificationService(WorkDoneProperties properties) {
        this.properties = properties;
    }

    public MatchingBand classify(double score) {
        if (score >= properties.matching().instantThreshold()) {
            return MatchingBand.INSTANT;
        }
        if (score >= properties.matching().digestThreshold()) {
            return MatchingBand.DIGEST;
        }
        if (score >= properties.matching().archiveThreshold()) {
            return MatchingBand.TRACK_ONLY;
        }
        return MatchingBand.ARCHIVE;
    }

    public OfferStatus toStatus(MatchingBand band) {
        return band == MatchingBand.ARCHIVE ? OfferStatus.ARCHIVED : OfferStatus.ANALYZED;
    }
}