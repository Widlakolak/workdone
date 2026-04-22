package com.workdone.backend.storage.entity;

import com.workdone.backend.model.OfferStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Encja bazy danych do przechowywania ofert pracy. 
 * To jest to, co faktycznie ląduje w PostgreSQL-u.
 */
@Entity
@Table(name = "job_offers")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class JobOfferEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id; // Unikalne ID oferty w mojej bazie

    @Column(name = "source_url", nullable = false, unique = true)
    private String sourceUrl; // Oryginalny URL oferty (musi być unikalny)

    @Column(nullable = false, unique = true)
    private String fingerprint; // Unikalny "odcisk palca" oferty (tytuł+firma+lokalizacja)

    private String title;
    private String company;
    private String location;

    @Column(name = "matching_score")
    private Double matchingScore; // Wynik dopasowania do CV

    @Column(name = "priority_score")
    private Double priorityScore; // Finalny priorytet

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OfferStatus status; // Status oferty (NEW, ANALYZED, APPLIED itp.)

    @Column(columnDefinition = "TEXT")
    private String content; // Pełna treść oferty (do analizy AI)

    @Column(columnDefinition = "jsonb")
    private String metadata; // Dodatkowe dane w formacie JSONB (np. specyficzne dla danego portalu)

    @Column(name = "published_at")
    private LocalDateTime publishedAt; // Data publikacji oferty

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt; // Kiedy oferta została dodana do mojej bazy

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt; // Kiedy oferta była ostatnio aktualizowana
}
