package com.workdone.backend.joboffer.ingestion.jooble;

import com.workdone.backend.joboffer.analysis.TechStackExtractor;
import com.workdone.backend.common.model.JobOfferRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Surowe dane z API Jooble na wewnętrzny model JobOfferRecord.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class JoobleMapper {

    @Autowired
    protected TechStackExtractor techStackExtractor;

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID().toString())")
    @Mapping(target = "fingerprint", ignore = true)
    @Mapping(target = "title", source = "title", defaultValue = "")
    @Mapping(target = "companyName", source = "company", defaultValue = "")
    @Mapping(target = "sourceUrl", source = "link")
    @Mapping(target = "location", source = "location", defaultValue = "")
    @Mapping(target = "rawDescription", source = "snippet", defaultValue = "")
    @Mapping(target = "salaryRange", source = "salary", defaultValue = "")
    @Mapping(target = "techStack", expression = "java(extractTechStack(job))")
    @Mapping(target = "matchingScore", ignore = true)
    @Mapping(target = "priorityScore", ignore = true)
    @Mapping(target = "status", expression = "java(com.workdone.backend.common.model.OfferStatus.NEW)")
    @Mapping(target = "publishedAt", source = "updated", qualifiedByName = "parseDate")
    @Mapping(target = "sourcePlatform", constant = "JOOBLE")
    public abstract JobOfferRecord toDomain(JoobleResponse.JoobleJob job);

    @Named("parseDate")
    protected LocalDateTime parseDate(String updated) {
        try {
            return updated != null ? LocalDateTime.parse(updated) : null;
        } catch (Exception e) {
            return null;
        }
    }

    protected List<String> extractTechStack(JoobleResponse.JoobleJob job) {
        // Używam teraz centralnego ekstraktora, żeby nie duplikować listy słów kluczowych
        return techStackExtractor.extract(job.title(), job.snippet());
    }
}
