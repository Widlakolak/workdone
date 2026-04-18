package com.workdone.backend.analysis;

import com.workdone.backend.config.WorkDoneProperties;
import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.profile.service.CvAggregationService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class OfferMatchingService {

    private final WorkDoneProperties properties;
    private final CvAggregationService cvAggregationService;

    private volatile String cachedProfile;

    public OfferMatchingService(WorkDoneProperties properties,
                                CvAggregationService cvAggregationService) {
        this.properties = properties;
        this.cvAggregationService = cvAggregationService;
    }

    private String getProfile() {
        if (cachedProfile == null) {
            synchronized (this) {
                if (cachedProfile == null) {
                    cachedProfile = cvAggregationService.buildMergedProfile();
                }
            }
        }
        return cachedProfile;
    }

    public double quickScore(JobOfferRecord offer) {
        return score(offer);
    }

    public double score(JobOfferRecord offer) {
        String profile = getProfile();

        String context = (profile + " " + offer.rawDescription() + " " + offer.techStack())
                .toLowerCase(Locale.ROOT);

        List<String> mustHave = properties.matching().mustHaveKeywords();
        if (mustHave == null || mustHave.isEmpty()) {
            return 50.0;
        }

        long matched = mustHave.stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .filter(context::contains)
                .count();

        double ratio = (double) matched / mustHave.size();
        return Math.min(100.0, Math.round((40 + ratio * 60) * 10.0) / 10.0);
    }

    public boolean passesMustHave(JobOfferRecord offer) {
        List<String> mustHave = properties.matching().mustHaveKeywords();
        if (mustHave == null || mustHave.isEmpty()) {
            return true;
        }

        String context = (offer.title() + " " + offer.rawDescription() + " " + offer.techStack())
                .toLowerCase(Locale.ROOT);

        return mustHave.stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .allMatch(context::contains);
    }
}