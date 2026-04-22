package com.workdone.backend.storage;

import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.model.OfferStatus;
import com.workdone.backend.storage.entity.JobOfferEntity;
import com.workdone.backend.storage.mapper.JobOfferMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class PersistentOfferStore implements OfferStore {

    private final JobOfferRepository repository;
    private final JobOfferMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySourceOrFingerprint(JobOfferRecord offer) {
        return repository.existsBySourceUrlOrFingerprint(offer.sourceUrl(), offer.fingerprint());
    }

    @Override
    @Transactional
    public void upsert(JobOfferRecord offer) {
        // Szukam oferty po URL albo fingerprint, żeby nie dodawać duplikatów
        Optional<JobOfferEntity> existingEntity = repository.findBySourceUrl(offer.sourceUrl())
                .or(() -> repository.findByFingerprint(offer.fingerprint()));

        JobOfferEntity entity;
        if (existingEntity.isPresent()) {
            entity = existingEntity.get();
            // Jak już jest, to aktualizuję tylko zmienne pola (mapper robi to za mnie)
            mapper.updateEntity(offer, entity);
            log.debug("🔄 Aktualizacja oferty: {}", entity.getTitle());
        } else {
            // Jak nowa, to tworzę encję
            entity = mapper.toEntity(offer);
            log.debug("🆕 Zapis nowej oferty: {}", entity.getTitle());
        }

        // Zapisuję i od razu flushuję, żeby mieć pewność, że dane są w bazie (ważne przy szybkich pętlach)
        repository.saveAndFlush(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobOfferRecord> findForDigest(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        return repository.findByPublishedAtBetween(startOfDay, endOfDay).stream()
                .map(mapper::toRecord)
                .toList();
    }

    @Override
    @Transactional
    public boolean updateStatusBySourceUrl(String sourceUrl, OfferStatus newStatus) {
        return repository.findBySourceUrl(sourceUrl)
                .map(entity -> {
                    entity.setStatus(newStatus);
                    repository.saveAndFlush(entity);
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JobOfferRecord> findBySourceUrl(String sourceUrl) {
        return repository.findBySourceUrl(sourceUrl).map(mapper::toRecord);
    }
}
