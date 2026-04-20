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
    private final MustHaveGroupConfig groupConfig;

    private volatile String cachedProfile;

    public OfferMatchingService(WorkDoneProperties properties,
                                CvAggregationService cvAggregationService,
                                MustHaveGroupConfig groupConfig) {
        this.properties = properties;
        this.cvAggregationService = cvAggregationService;
        this.groupConfig = groupConfig;
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
                .map(k -> k.toLowerCase(Locale.ROOT))
                .filter(context::contains)
                .count();

        double ratio = (double) matched / mustHave.size();
        return Math.min(100.0, Math.round((40 + ratio * 60) * 10.0) / 10.0);
    }

    public boolean passesMustHave(JobOfferRecord offer) {

        String context = (offer.title() + " " + offer.rawDescription() + " " + offer.techStack())
                .toLowerCase(Locale.ROOT);

        List<MustHaveGroup> groups = groupConfig.groups();

        int matchedGroups = 0;

        for (MustHaveGroup group : groups) {
            boolean matched = containsAny(context, group.keywords());

            if (matched) {
                matchedGroups++;
            } else if (group.required()) {
                return false;
            }
        }

        return matchedGroups >= groupConfig.minGroupsToPass();
    }

    private boolean containsAny(String text, List<String> keywords) {
        return keywords.stream()
                .anyMatch(k -> text.contains(normalize(k)));
    }

    private String normalize(String s) {
        return s.toLowerCase()
                .replace("-", "")
                .replace(" ", "");
    }
}