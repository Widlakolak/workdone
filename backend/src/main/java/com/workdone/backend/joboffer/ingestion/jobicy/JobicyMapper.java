package com.workdone.backend.joboffer.ingestion.jobicy;

import com.workdone.backend.joboffer.analysis.TechStackExtractor;
import com.workdone.backend.common.model.JobOfferRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Przerabiam DTO-sy z Jobicy na mój wewnętrzny format. 
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class JobicyMapper {

    @Autowired
    protected TechStackExtractor techStackExtractor;

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID().toString())")
    @Mapping(target = "fingerprint", ignore = true)
    @Mapping(target = "title", source = "jobTitle")
    @Mapping(target = "companyName", source = "companyName")
    @Mapping(target = "sourceUrl", source = "url")
    @Mapping(target = "location", source = "jobGeo")
    @Mapping(target = "rawDescription", source = "jobDescription")
    @Mapping(target = "salaryRange", expression = "java(formatSalary(dto))")
    @Mapping(target = "techStack", expression = "java(extractTechStack(dto))")
    @Mapping(target = "matchingScore", ignore = true)
    @Mapping(target = "priorityScore", ignore = true)
    @Mapping(target = "status", expression = "java(com.workdone.backend.common.model.OfferStatus.NEW)")
    @Mapping(target = "publishedAt", source = "pubDate", qualifiedByName = "parseDateTime")
    @Mapping(target = "sourcePlatform", constant = "JOBICY")
    public abstract JobOfferRecord toDomain(JobicyJobDto dto);

    @Named("parseDateTime")
    protected LocalDateTime parseDateTime(String pubDate) {
        if (pubDate == null || pubDate.isBlank()) return null;
        try {
            return OffsetDateTime.parse(pubDate, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }

    protected String formatSalary(JobicyJobDto dto) {
        if (dto.getSalaryMin() == null && dto.getSalaryMax() == null) return null;
        
        StringBuilder sb = new StringBuilder();
        if (dto.getSalaryMin() != null) sb.append(dto.getSalaryMin());
        if (dto.getSalaryMin() != null && dto.getSalaryMax() != null) sb.append(" - ");
        if (dto.getSalaryMax() != null) sb.append(dto.getSalaryMax());
        if (dto.getSalaryCurrency() != null) sb.append(" ").append(dto.getSalaryCurrency());
        if (dto.getSalaryPeriod() != null) sb.append(" / ").append(dto.getSalaryPeriod());
        
        return sb.toString();
    }

    protected List<String> extractTechStack(JobicyJobDto dto) {
        // Tu też leci TechStackExtractor, jedna lista technologii do pilnowania
        return techStackExtractor.extract(dto.getJobTitle(), dto.getJobDescription());
    }
}
