package com.workdone.backend.storage;

import com.workdone.backend.model.OfferStatus;
import com.workdone.backend.storage.entity.JobOfferEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Standardowe repozytorium Spring Data JPA do obsługi ofert w bazie SQL. 
 * Zawiera kilka specyficznych metod do szukania duplikatów i ofert z danego dnia.
 */
@Repository
public interface JobOfferRepository extends JpaRepository<JobOfferEntity, UUID> {
    
    boolean existsBySourceUrlOrFingerprint(String sourceUrl, String fingerprint);
    
    Optional<JobOfferEntity> findBySourceUrl(String sourceUrl);
    
    List<JobOfferEntity> findByPublishedAtBetween(LocalDateTime start, LocalDateTime end);

    Optional<JobOfferEntity> findByFingerprint(String fingerprint);

    // Szukam ofert o konkretnym statusie
    List<JobOfferEntity> findByStatus(OfferStatus status);
}
