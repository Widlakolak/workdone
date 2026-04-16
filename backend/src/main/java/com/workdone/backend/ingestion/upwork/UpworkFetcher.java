package com.workdone.backend.ingestion.upwork;

import com.workdone.backend.ingestion.JobProvider;
import com.workdone.backend.model.JobOfferRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "workdone.providers.upwork", name = "enabled", havingValue = "true")
public class UpworkFetcher implements JobProvider {

    @Override
    public String sourceName() {
        return "UPWORK";
    }

    @Override
    public List<JobOfferRecord> fetchOffers() {
        return List.of();
    }
}