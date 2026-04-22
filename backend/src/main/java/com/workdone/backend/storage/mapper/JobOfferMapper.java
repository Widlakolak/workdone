package com.workdone.backend.storage.mapper;

import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.storage.entity.JobOfferEntity;
import org.mapstruct.*;

import java.util.UUID;

/**
 * Mapper między moim modelem biznesowym (JobOfferRecord) a modelem bazodanowym (JobOfferEntity). 
 * MapStruct generuje implementację, oszczędzając mi pisania nudnych getterów i setterów.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface JobOfferMapper {

    @Mapping(target = "company", source = "companyName")
    @Mapping(target = "content", source = "rawDescription")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    JobOfferEntity toEntity(JobOfferRecord record);

    @Mapping(target = "companyName", source = "company")
    @Mapping(target = "rawDescription", source = "content")
    @Mapping(target = "techStack", ignore = true)
    @Mapping(target = "salaryRange", ignore = true)
    @Mapping(target = "sourcePlatform", ignore = true)
    JobOfferRecord toRecord(JobOfferEntity entity);

    /**
     * Aktualizuję istniejącą encję nowymi danymi z rekordu. 
     * Ignoruję ID i daty utworzenia, żeby nie popsuć integralności bazy.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "company", source = "companyName")
    @Mapping(target = "content", source = "rawDescription")
    void updateEntity(JobOfferRecord record, @MappingTarget JobOfferEntity entity);

    default UUID map(String value) {
        return value != null ? UUID.fromString(value) : null;
    }

    default String map(UUID value) {
        return value != null ? value.toString() : null;
    }
}
