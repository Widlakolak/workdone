package com.workdone.backend.ingestion.theprotocol;

import com.workdone.backend.ingestion.JobProvider;
import com.workdone.backend.model.JobOfferRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "workdone.providers.theprotocol", name = "enabled", havingValue = "true")
public class TheProtocolFetcher implements JobProvider {

    @Override
    public String sourceName() {
        return "THE_PROTOCOL";
    }

    @Override
    public List<JobOfferRecord> fetchOffers() {
        return List.of();
    }
}