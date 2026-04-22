package com.workdone.backend.storage;

import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.model.OfferStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Interfejs do zarządzania ofertami pracy. 
 * Abstrahuje dostęp do bazy danych, żeby łatwo było podmienić implementację (np. z SQL na NoSQL).
 */
public interface OfferStore {
    // Sprawdzam, czy oferta już istnieje (po URL-u lub "odcisku palca")
    boolean existsBySourceOrFingerprint(JobOfferRecord offer);
    
    // Dodaję nową ofertę lub aktualizuję istniejącą
    void upsert(JobOfferRecord offer);
    
    // Pobieram oferty do dziennego podsumowania
    List<JobOfferRecord> findForDigest(LocalDate date);
    
    // Zmieniam status oferty (np. na "APPLIED" po kliknięciu w Discordzie)
    boolean updateStatusBySourceUrl(String sourceUrl, OfferStatus newStatus);
    
    // Szukam konkretnej oferty po jej URL-u
    Optional<JobOfferRecord> findBySourceUrl(String sourceUrl);
}
