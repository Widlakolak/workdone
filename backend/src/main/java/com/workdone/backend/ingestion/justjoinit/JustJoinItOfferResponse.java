package com.workdone.backend.ingestion.justjoinit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record JustJoinItOfferResponse(
        String id,
        String title,
        String companyName,
        String openToHireUkrainians,
        String city,
        String workplaceType,
        String remoteInterview,
        List<JustJoinItSkillResponse> skills,
        JustJoinItSalaryResponse employmentTypes,
        String publishedAt,
        String url,
        String markerIcon
) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record JustJoinItSkillResponse(String name) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record JustJoinItSalaryResponse(String type, Salary fromPln, Salary toPln) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Salary(Integer value) {
    }
}