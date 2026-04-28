package com.workdone.backend.joboffer.storage;

import com.workdone.backend.common.model.JobOfferRecord;
import com.workdone.backend.common.model.OfferStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prosta baza danych w pamięci RAM, używana tylko w testach. 
 * Dzięki temu testy biegają błyskawicznie i nie potrzebują prawdziwego Postgresa.
 */
@Slf4j
@Component
@Profile("test")
public class InMemoryOfferStore implements OfferStore {

    private final Map<String, JobOfferRecord> bySourceUrl = new ConcurrentHashMap<>();
    private final Map<String, JobOfferRecord> byFingerprint = new ConcurrentHashMap<>();

    @Override
    public boolean existsBySourceOrFingerprint(JobOfferRecord offer) {
        // Sprawdzam, czy mam już taką ofertę w mapach
        return bySourceUrl.containsKey(offer.sourceUrl()) || byFingerprint.containsKey(offer.fingerprint());
    }

    @Override
    public void upsert(JobOfferRecord offer) {
        bySourceUrl.put(offer.sourceUrl(), offer);
        byFingerprint.put(offer.fingerprint(), offer);
    }

    @Override
    public List<JobOfferRecord> findForDigest(LocalDate today) {
        return bySourceUrl.values().stream()
                .filter(offer -> offer.publishedAt() != null)
                .filter(offer -> offer.publishedAt().toLocalDate().equals(today))
                .toList();
    }

    @Override
    public boolean updateStatusBySourceUrl(String sourceUrl, OfferStatus newStatus) {
        JobOfferRecord current = bySourceUrl.get(sourceUrl);
        if (current == null) return false;

        JobOfferRecord updated = current.withStatus(newStatus);
        bySourceUrl.put(sourceUrl, updated);
        byFingerprint.put(updated.fingerprint(), updated);
        return true;
    }

    @Override
    public Optional<JobOfferRecord> findBySourceUrl(String sourceUrl) {
        return Optional.ofNullable(bySourceUrl.get(sourceUrl));
    }

    @Override
    public List<JobOfferRecord> findByStatus(OfferStatus status) {
        return bySourceUrl.values().stream()
                .filter(offer -> offer.status() == status)
                .toList();
    }
}
