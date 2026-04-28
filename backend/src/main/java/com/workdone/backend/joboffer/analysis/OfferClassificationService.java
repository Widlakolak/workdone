package com.workdone.backend.joboffer.analysis;

import com.workdone.backend.common.model.OfferStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfferClassificationService {

    private final DynamicConfigService dynamicConfig;

    public MatchingBand classify(double score) {
        if (score >= dynamicConfig.getInstantThreshold()) return MatchingBand.INSTANT;
        if (score >= dynamicConfig.getDigestThreshold()) return MatchingBand.DIGEST;
        if (score >= dynamicConfig.getArchiveThreshold()) return MatchingBand.TRACKING;
        return MatchingBand.ARCHIVED;
    }

    public OfferStatus toStatus(MatchingBand band) {
        return switch (band) {
            case INSTANT -> OfferStatus.ANALYZED; // Natychmiast wysłane, czekają na decyzję
            case DIGEST -> OfferStatus.ANALYZED;   // Wysłane w podsumowaniu, czekają na decyzję
            case TRACKING -> OfferStatus.ANALYZED; // Śledzone, czekają na decyzję
            case ARCHIVED -> OfferStatus.ARCHIVED; // Odrzucone przez system
        };
    }
}
