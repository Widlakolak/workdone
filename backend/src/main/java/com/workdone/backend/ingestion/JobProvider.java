package com.workdone.backend.ingestion;

import com.workdone.backend.model.JobOfferRecord;

import java.util.List;

/**
 * Wspólny interfejs dla każdego źródła ofert (np. API, RSS). 
 * Każdy dostawca musi mieć nazwę i umieć pobrać listę ofert.
 */
public interface JobProvider {

    String sourceName();

    List<JobOfferRecord> fetchOffers();
}
