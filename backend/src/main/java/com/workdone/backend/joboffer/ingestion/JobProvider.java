package com.workdone.backend.joboffer.ingestion;

import com.workdone.backend.common.model.JobOfferRecord;

import java.util.List;

/**
 * Wspólny interfejs dla każdego źródła ofert (np. API, RSS). 
 * Każdy dostawca musi mieć nazwę i umieć pobrać listę ofert dla danego kontekstu.
 */
public interface JobProvider {

    String sourceName();

    List<JobOfferRecord> fetchOffers(SearchContext context);
}
