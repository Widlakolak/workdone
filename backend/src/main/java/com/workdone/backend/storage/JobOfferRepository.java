package com.workdone.backend.storage;

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
    
    // Sprawdzam, czy oferta o takim URL-u albo treści już u mnie jest
    boolean existsBySourceUrlOrFingerprint(String sourceUrl, String fingerprint);
    
    // Szukam po URL-u (np. przy aktualizacji statusu z Discorda)
    Optional<JobOfferEntity> findBySourceUrl(String sourceUrl);
    
    // Wyciągam oferty z konkretnego przedziału czasu (przydatne do raportów dziennych)
    List<JobOfferEntity> findByPublishedAtBetween(LocalDateTime start, LocalDateTime end);

    // Szukam po unikalnym "odcisku palca" treści
    Optional<JobOfferEntity> findByFingerprint(String fingerprint);
}
