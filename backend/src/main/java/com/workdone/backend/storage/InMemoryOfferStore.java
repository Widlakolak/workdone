package com.workdone.backend.storage;

import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.model.OfferStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryOfferStore {

    private final Map<String, JobOfferRecord> bySourceUrl = new ConcurrentHashMap<>();
    private final Map<String, JobOfferRecord> byFingerprint = new ConcurrentHashMap<>();

    public void saveIfNew(JobOfferRecord offer) {
        bySourceUrl.putIfAbsent(offer.sourceUrl(), offer);
        byFingerprint.putIfAbsent(offer.fingerprint(), offer);
    }

    public boolean existsBySourceOrFingerprint(JobOfferRecord offer) {
        return bySourceUrl.containsKey(offer.sourceUrl()) || byFingerprint.containsKey(offer.fingerprint());
    }

    public void upsert(JobOfferRecord offer) {
        bySourceUrl.put(offer.sourceUrl(), offer);
        byFingerprint.put(offer.fingerprint(), offer);
    }

    public boolean updateStatusBySourceUrl(String sourceUrl, OfferStatus newStatus) {
        JobOfferRecord current = bySourceUrl.get(sourceUrl);
        if (current == null) {
            return false;
        }

        JobOfferRecord updated = current.withAnalysis(current.matchingScore(), newStatus);
        bySourceUrl.put(sourceUrl, updated);
        byFingerprint.put(updated.fingerprint(), updated);
        return true;
    }

    public Collection<JobOfferRecord> all() {
        return bySourceUrl.values();
    }

    public List<JobOfferRecord> findForDigest(LocalDate today) {
        return bySourceUrl.values().stream()
                .filter(offer -> offer.publishedAt() != null)
                .filter(offer -> offer.publishedAt().toLocalDate().equals(today))
                .toList();
    }

    public List<JobOfferRecord> findByUrls(List<String> urls) {
        List<JobOfferRecord> result = new ArrayList<>();
        for (String url : urls) {
            JobOfferRecord offer = bySourceUrl.get(url);
            if (offer != null) {
                result.add(offer);
            }
        }
        return result;
    }
}