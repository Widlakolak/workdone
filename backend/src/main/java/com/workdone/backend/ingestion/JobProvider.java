package com.workdone.backend.ingestion;

import com.workdone.backend.model.JobOfferRecord;

import java.util.List;

public interface JobProvider {

    String sourceName();

    List<JobOfferRecord> fetchOffers();
}