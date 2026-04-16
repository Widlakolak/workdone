package com.workdone.backend.ingestion.nofluffjobs;

import com.workdone.backend.ingestion.JobProvider;
import com.workdone.backend.model.JobOfferRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "workdone.providers.nofluffjobs", name = "enabled", havingValue = "true")
public class NoFluffJobsFetcher implements JobProvider {

    @Override
    public String sourceName() {
        return "NO_FLUFF_JOBS";
    }

    @Override
    public List<JobOfferRecord> fetchOffers() {
        return List.of();
    }
}