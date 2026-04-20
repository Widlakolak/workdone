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

    public MatchingBand classify(Double score) {
        if (score == null) return MatchingBand.ARCHIVE;
        if (score >= properties.matching().instantThreshold()) return MatchingBand.INSTANT;
        if (score >= properties.matching().digestThreshold()) return MatchingBand.DIGEST;
        if (score >= properties.matching().archiveThreshold()) return MatchingBand.TRACK_ONLY;
        return MatchingBand.ARCHIVE;
    }

    public OfferStatus toStatus(MatchingBand band) {
        return switch (band) {
            case ARCHIVE -> OfferStatus.ARCHIVED;
            case INSTANT, DIGEST, TRACK_ONLY -> OfferStatus.ANALYZED;
        };
    }
}