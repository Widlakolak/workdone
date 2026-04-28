package com.workdone.backend.joboffer.ingestion.jooble;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JoobleResponse(
        int totalCount,
        List<JoobleJob> jobs
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JoobleJob(
            String id,
            String title,
            String company,
            String location,
            String snippet,
            String salary,
            String link,
            String source,
            String type,
            String updated
    ) {}
}