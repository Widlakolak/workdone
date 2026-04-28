package com.workdone.backend.joboffer.ingestion;

import lombok.Builder;
import java.util.List;

/**
 * Obiekt trzymający wszystkie parametry wyszukiwania, 
 * żeby nie przekazywać ich "luzem" do dostawców ofert.
 */
@Builder
public record SearchContext(
        List<String> keywords, // Słowa kluczowe (np. Java, Spring)
        String location,       // Gdzie szukamy (np. Polska)
        boolean remoteOnly,    // Czy tylko oferty zdalne
        Integer maxResults,    // Limit wyników na jedno zapytanie
        String industry        // Branża (przydatne np. dla API Jobicy)
) {
    public static final String REMOTE_GLOBAL = "REMOTE_GLOBAL";

    public String getQueryString() {
        return String.join(" ", keywords);
    }

    public boolean isGlobalRemote() {
        return REMOTE_GLOBAL.equalsIgnoreCase(location);
    }
}