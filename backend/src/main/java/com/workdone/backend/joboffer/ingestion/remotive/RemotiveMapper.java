package com.workdone.backend.joboffer.ingestion.remotive;

import com.workdone.backend.joboffer.analysis.TechStackExtractor;
import com.workdone.backend.common.model.JobOfferRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class RemotiveMapper {

    @Autowired
    protected TechStackExtractor techStackExtractor;

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID().toString())")
    @Mapping(target = "fingerprint", ignore = true)
    @Mapping(target = "title", source = "title")
    @Mapping(target = "companyName", source = "companyName")
    @Mapping(target = "sourceUrl", source = "url")
    @Mapping(target = "location", source = "candidateRequiredLocation")
    @Mapping(target = "rawDescription", source = "description")
    @Mapping(target = "salaryRange", source = "salary")
    @Mapping(target = "techStack", expression = "java(extractTechStack(dto))")
    @Mapping(target = "matchingScore", ignore = true)
    @Mapping(target = "priorityScore", ignore = true)
    @Mapping(target = "status", expression = "java(com.workdone.backend.common.model.OfferStatus.NEW)")
    @Mapping(target = "publishedAt", source = "publicationDate", qualifiedByName = "parseRemotiveDateTime")
    @Mapping(target = "sourcePlatform", constant = "REMOTIVE")
    public abstract JobOfferRecord toDomain(RemotiveJobDto dto);

    @Named("parseRemotiveDateTime")
    protected LocalDateTime parseRemotiveDateTime(String pubDate) {
        if (pubDate == null || pubDate.isBlank()) return null;
        try {
            // Remotive format: 2020-02-15T10:23:26
            return LocalDateTime.parse(pubDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }

    protected List<String> extractTechStack(RemotiveJobDto dto) {
        return techStackExtractor.extract(dto.getTitle(), dto.getDescription());
    }
}
