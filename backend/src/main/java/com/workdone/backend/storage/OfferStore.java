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
    boolean existsBySourceOrFingerprint(JobOfferRecord offer);
    
    void upsert(JobOfferRecord offer);
    
    List<JobOfferRecord> findForDigest(LocalDate date);
    
    boolean updateStatusBySourceUrl(String sourceUrl, OfferStatus newStatus);
    
    Optional<JobOfferRecord> findBySourceUrl(String sourceUrl);

    // Pobieram oferty o konkretnym statusie (np. ANALYZED - czekające na decyzję)
    List<JobOfferRecord> findByStatus(OfferStatus status);
}
