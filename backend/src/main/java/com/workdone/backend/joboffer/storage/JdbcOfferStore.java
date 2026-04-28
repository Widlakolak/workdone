package com.workdone.backend.joboffer.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workdone.backend.common.model.JobOfferRecord;
import com.workdone.backend.common.model.OfferStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
@Profile("!test")
public class JdbcOfferStore implements OfferStore {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean existsBySourceOrFingerprint(JobOfferRecord offer) {
        String sql = "SELECT COUNT(*) FROM job_offers WHERE fingerprint = ? OR source_url = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, offer.fingerprint(), offer.sourceUrl());
        return count != null && count > 0;
    }

    @Override
    public void upsert(JobOfferRecord offer) {
        String metadataJson = "{}";
        try {
            metadataJson = objectMapper.writeValueAsString(Map.of(
                    "company", offer.companyName() != null ? offer.companyName() : "",
                    "sourcePlatform", offer.sourcePlatform() != null ? offer.sourcePlatform() : ""
            ));
        } catch (JsonProcessingException e) {
            log.error("Błąd serializacji metadanych dla oferty {}: {}", offer.id(), e.getMessage());
        }

        String sql = """
            INSERT INTO job_offers (id, fingerprint, title, company, location, matching_score, priority_score, status,
                                    source_url, raw_description, tech_stack, published_at, metadata, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?::jsonb, NOW(), NOW())
            ON CONFLICT (fingerprint) DO UPDATE SET
                title = EXCLUDED.title,
                company = EXCLUDED.company,
                location = EXCLUDED.location,
                matching_score = EXCLUDED.matching_score,
                priority_score = EXCLUDED.priority_score,
                status = EXCLUDED.status,
                source_url = EXCLUDED.source_url,
                raw_description = EXCLUDED.raw_description,
                tech_stack = EXCLUDED.tech_stack,
                published_at = EXCLUDED.published_at,
                metadata = EXCLUDED.metadata,
                updated_at = NOW()
            WHERE job_offers.priority_score < EXCLUDED.priority_score OR job_offers.status != EXCLUDED.status
            """;

        jdbcTemplate.update(sql,
                offer.id() != null ? UUID.fromString(offer.id()) : UUID.randomUUID(), // Użyj istniejącego ID lub wygeneruj nowe
                offer.fingerprint(),
                offer.title(),
                offer.companyName(),
                offer.location(),
                offer.matchingScore(),
                offer.priorityScore(),
                offer.status().name(),
                offer.sourceUrl(),
                offer.rawDescription(),
                offer.techStack() != null ? toJsonb(offer.techStack()) : "[]", // Konwersja List<String> na JSONB
                offer.publishedAt() != null ? offer.publishedAt() : LocalDateTime.now(),
                metadataJson
        );
    }

    @Override
    public List<JobOfferRecord> findForDigest(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        String sql = "SELECT * FROM job_offers WHERE published_at BETWEEN ? AND ?";
        return jdbcTemplate.query(sql, this::mapRowToJobOfferRecord, startOfDay, endOfDay);
    }

    @Override
    public boolean updateStatusBySourceUrl(String sourceUrl, OfferStatus newStatus) {
        String sql = "UPDATE job_offers SET status = ?, updated_at = NOW() WHERE source_url = ?";
        int updatedRows = jdbcTemplate.update(sql, newStatus.name(), sourceUrl);
        return updatedRows > 0;
    }

    @Override
    public Optional<JobOfferRecord> findBySourceUrl(String sourceUrl) {
        String sql = "SELECT * FROM job_offers WHERE source_url = ?";
        return jdbcTemplate.query(sql, this::mapRowToJobOfferRecord, sourceUrl).stream().findFirst();
    }

    @Override
    public List<JobOfferRecord> findByStatus(OfferStatus status) {
        String sql = "SELECT * FROM job_offers WHERE status = ?";
        return jdbcTemplate.query(sql, this::mapRowToJobOfferRecord, status.name());
    }

    private JobOfferRecord mapRowToJobOfferRecord(ResultSet rs, int rowNum) throws SQLException {
        return JobOfferRecord.builder()
                .id(rs.getString("id"))
                .fingerprint(rs.getString("fingerprint"))
                .title(rs.getString("title"))
                .companyName(rs.getString("company"))
                .location(rs.getString("location"))
                .matchingScore(rs.getDouble("matching_score"))
                .priorityScore(rs.getDouble("priority_score"))
                .status(OfferStatus.valueOf(rs.getString("status")))
                .sourceUrl(rs.getString("source_url"))
                .rawDescription(rs.getString("raw_description"))
                .techStack(fromJsonb(rs.getString("tech_stack")))
                .publishedAt(rs.getTimestamp("published_at").toLocalDateTime())
                .sourcePlatform(extractSourcePlatformFromMetadata(rs.getString("metadata")))
                .build();
    }

    // Helper do konwersji List<String> na JSONB string
    private String toJsonb(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("Błąd konwersji List<String> na JSONB: {}", e.getMessage());
            return "[]";
        }
    }

    // Helper do konwersji JSONB string na List<String>
    private List<String> fromJsonb(String jsonb) {
        try {
            return objectMapper.readValue(jsonb, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            log.warn("Błąd konwersji JSONB na List<String>: {}", e.getMessage());
            return List.of();
        }
    }

    // Helper do ekstrakcji sourcePlatform z JSONB metadata
    private String extractSourcePlatformFromMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> metadata = objectMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {});
            Object platform = metadata.get("sourcePlatform");
            return platform != null ? platform.toString() : null;
        } catch (JsonProcessingException e) {
            log.warn("Błąd parsowania metadanych JSONB: {}", e.getMessage());
            return null;
        }
    }
}