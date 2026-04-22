package com.workdone.backend.ingestion.jobicy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobicyResponse {
    private String apiVersion;
    private int jobCount;
    private List<JobicyJobDto> jobs;
    private boolean success;
}

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class JobicyJobDto {
    private Long id;
    private String url;
    private String jobTitle;
    private String companyName;
    private String companyLogo;
    private List<String> jobIndustry;
    private List<String> jobType;
    private String jobGeo;
    private String jobLevel;
    private String jobExcerpt;
    private String jobDescription;
    private String pubDate; // ISO-8601 (np. 2026-04-22T01:56:05+00:00)
    
    private String salaryMin;
    private String salaryMax;
    private String salaryCurrency;
    private String salaryPeriod;
}
