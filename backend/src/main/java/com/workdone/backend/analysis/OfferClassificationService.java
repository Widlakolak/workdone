package com.workdone.backend.analysis;

import com.workdone.backend.model.OfferStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfferClassificationService {

    private final DynamicConfigService dynamicConfig;

    public MatchingBand classify(double score) {
        // Przypisuję ofertę do "kubełka" na podstawie punktacji. 
        // Progi mogę zmieniać w locie przez bota, więc pobieram je z DynamicConfigService.
        if (score >= dynamicConfig.getInstantThreshold()) return MatchingBand.INSTANT; // Najlepsze, powiadomienie od razu
        if (score >= dynamicConfig.getDigestThreshold()) return MatchingBand.DIGEST;   // Dobre, trafią do raportu dziennego
        if (score >= dynamicConfig.getArchiveThreshold()) return MatchingBand.TRACKING; // Słabe, ale zostawiam w bazie do śledzenia
        return MatchingBand.ARCHIVED; // Odpady, chowam do archiwum
    }

    public OfferStatus toStatus(MatchingBand band) {
        return switch (band) {
            case INSTANT -> OfferStatus.NEW;
            case DIGEST -> OfferStatus.NEW;
            case TRACKING -> OfferStatus.ANALYZED;
            case ARCHIVED -> OfferStatus.ARCHIVED;
        };
    }
}
