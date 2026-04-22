package com.workdone.backend.analysis;

import com.workdone.backend.profile.service.CandidateProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MustHaveGroupConfig {

    private final CandidateProfileService candidateProfileService;

    // Mapuję poziomy doświadczenia na konkretne frazy, których szukam w ogłoszeniach
    private static final Map<String, List<String>> SENIORITY_MAP = Map.of(
            "junior", List.of("junior", "intern", "trainee", "entry", "student", "młodszy"),
            "mid", List.of("mid", "regular", "professional", "samodzielny"),
            "senior", List.of("senior", "lead", "architect", "principal", "starszy", "ekspert")
    );

    public List<MustHaveGroup> groups() {
        List<MustHaveGroup> allGroups = new ArrayList<>();

        // Moja baza - jak nie ma Javy/Kotlina, to w ogóle nie patrzę na ofertę
        allGroups.add(new MustHaveGroup("language", List.of(
                "java", "java 17", "java 21", "jdk", "jre", "kotlin"
        ), true));

        // Spring to mus - bez tego nie ruszam projektów backendowych
        allGroups.add(new MustHaveGroup("framework", List.of(
                "spring", "spring boot", "springboot", "spring-boot",
                "spring mvc", "spring security", "spring data"
        ), true));

        // Bazy danych są ważne, ale mogę się douczyć konkretnej, więc to nie jest twardy wymóg
        allGroups.add(new MustHaveGroup("database", List.of(
                "sql", "postgres", "postgresql", "mysql", "oracle",
                "elasticsearch", "hibernate", "jpa"
        ), false));

        // Dorzucam to, co AI wyciągnęło z mojego CV jako moje unikalne skille
        List<String> cvKeywords = candidateProfileService.getSuggestedKeywords();
        if (cvKeywords != null && !cvKeywords.isEmpty()) {
            allGroups.add(new MustHaveGroup("cv_skills", cvKeywords, false));
        }

        // Staram się dopasować poziom oferty do tego, co mam w CV
        String rawSeniority = candidateProfileService.getSeniority();
        if (rawSeniority != null) {
            String key = rawSeniority.toLowerCase();
            String normalizedKey = null;
            
            if (key.contains("junior") || key.contains("intern")) normalizedKey = "junior";
            else if (key.contains("mid") || key.contains("regular") || key.contains("professional")) normalizedKey = "mid";
            else if (key.contains("senior") || key.contains("lead") || key.contains("arch")) normalizedKey = "senior";

            if (normalizedKey != null) {
                List<String> keywords = SENIORITY_MAP.get(normalizedKey);
                if (keywords != null) {
                    allGroups.add(new MustHaveGroup("seniority", keywords, false));
                }
            }
        }

        return allGroups;
    }

    public int minGroupsToPass() {
        // Dynamiczny próg: im więcej mam grup zdefiniowanych, tym więcej wymagam od oferty
        int totalGroups = groups().size();
        return totalGroups >= 4 ? 3 : 2;
    }
}
