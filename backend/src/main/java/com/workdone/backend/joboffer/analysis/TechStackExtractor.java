package com.workdone.backend.joboffer.analysis;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Locale;

/**
 * Wyciągam technologie z tekstu, żeby nie klepać tego samego w każdym maperze.
 * Tu sprawdzam czy to nie duplikat, żeby nie bulić za AI ;) (notatka: to dotyczy tech stacku, ale pasuje do klimatu)
 */
@Component
public class TechStackExtractor {

    private static final List<String> KNOWN_TECH = List.of(
            "java", "spring", "spring boot", "hibernate", "jpa",
            "sql", "postgres", "mysql", "docker", "docker-compose", "kubernetes",
            "aws", "azure", "rest", "microservices", "python", "react", "angular", "node"
    );

    public List<String> extract(String... sources) {
        // Łączę wszystkie źródła (tytuł, opis itp.) w jeden tekst do przeszukania
        StringBuilder fullText = new StringBuilder();
        for (String source : sources) {
            if (source != null) {
                fullText.append(source).append(" ");
            }
        }
        
        String text = fullText.toString().toLowerCase(Locale.ROOT);
        
        return KNOWN_TECH.stream()
                .filter(text::contains)
                .toList();
    }
}
