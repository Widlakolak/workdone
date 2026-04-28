package com.workdone.backend.joboffer.orchestration;

import com.workdone.backend.joboffer.analysis.OfferFingerprintFactory;
import com.workdone.backend.common.model.JobOfferRecord;
import com.workdone.backend.common.model.OfferStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Czyścik do ofert. Zanim cokolwiek zrobię z ofertą, 
 * muszę wyczyścić śmieci z tytułu i nadać jej unikalny odcisk palca.
 */
@Component
@RequiredArgsConstructor
public class OfferEnricher {

    private final OfferFingerprintFactory fingerprintFactory;

    public JobOfferRecord cleanAndEnrich(JobOfferRecord offer) {
        // Wywalam dziwne białe znaki z tytułu i nazwy firmy
        String cleanTitle = offer.title() != null ? offer.title().replaceAll("\\s+", " ").trim() : "Bez tytułu";
        String cleanCompany = offer.companyName() != null ? offer.companyName().trim() : "Nieznana";

        // Jeśli oferta nie ma jeszcze ID, to jej nadaję. Generuję też fingerprint do wykrywania duplikatów.
        String id = (offer.id() == null || offer.id().isBlank()) ? UUID.randomUUID().toString() : offer.id();
        String fingerprint = fingerprintFactory.createFrom(cleanTitle, cleanCompany, offer.location());

        return offer.toBuilder()
                .id(id)
                .fingerprint(fingerprint)
                .title(cleanTitle)
                .companyName(cleanCompany)
                .status(OfferStatus.NEW)
                .build();
    }
}
