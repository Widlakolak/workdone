package com.workdone.backend.ingestion.remotive;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RemotiveResponse {
    @JsonProperty("job-count")
    private int jobCount;
    private List<RemotiveJobDto> jobs;
}

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class RemotiveJobDto {
    private Long id;
    private String url;
    private String title;
    @JsonProperty("company_name")
    private String companyName;
    private String category;
    @JsonProperty("job_type")
    private String jobType;
    @JsonProperty("publication_date")
    private String publicationDate; // Format: 2020-02-15T10:23:26
    @JsonProperty("candidate_required_location")
    private String candidateRequiredLocation;
    private String salary;
    private String description;
}
