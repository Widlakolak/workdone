package com.workdone.backend.analysis;

import com.workdone.backend.config.WorkDoneProperties;
import com.workdone.backend.profile.service.CandidateProfileService;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Getter
@Setter
public class DynamicConfigService {

    private final WorkDoneProperties properties;
    private final CandidateProfileService profileService;

    private double semanticThreshold;
    private double instantThreshold;
    private double digestThreshold;
    private double archiveThreshold;
    private List<String> mustHaveKeywords;
    private String preferredLocation;
    private boolean allowRemoteSearch;

    public DynamicConfigService(WorkDoneProperties properties, @Lazy CandidateProfileService profileService) {
        this.properties = properties;
        this.profileService = profileService;
    }

    @PostConstruct
    public void init() {
        // Ładujemy progi z propertiesów, żeby nie wbijać ich na sztywno
        this.semanticThreshold = properties.matching() != null ? properties.matching().semanticThreshold() : 0.7;
        this.instantThreshold = properties.matching() != null ? properties.matching().instantThreshold() : 0.8;
        this.digestThreshold = properties.matching() != null ? properties.matching().digestThreshold() : 0.6;
        this.archiveThreshold = properties.matching() != null ? properties.matching().archiveThreshold() : 0.4;
        this.mustHaveKeywords = new ArrayList<>(properties.matching() != null ? properties.matching().mustHaveKeywords() : List.of());
        
        // Gdzie szukamy ofert? Jak nic nie podam, to domyślnie Polska i Remote
        if (properties.search() != null) {
            this.preferredLocation = properties.search().defaultLocation();
            this.allowRemoteSearch = properties.search().allowRemote();
        } else {
            this.preferredLocation = "Poland";
            this.allowRemoteSearch = true;
        }

        log.info("⚙️ Konfiguracja dynamiczna zainicjalizowana: Semantic={}%, Location={}, Remote={}", 
                semanticThreshold, preferredLocation, allowRemoteSearch);
    }

    // Spinamy lokalizację z tym, co AI wygrzebało z mojego CV
    public void syncWithProfile() {
        String profileLocation = profileService.getLocation();
        if (profileLocation != null && !profileLocation.isBlank()) {
            log.info("🔄 Synchronizacja lokalizacji z profilu CV: {}", profileLocation);
            this.preferredLocation = profileLocation;
        }
    }

    public String updateConfig(String param, String value) {
        try {
            switch (param.toLowerCase()) {
                case "semantic": this.semanticThreshold = Double.parseDouble(value); break;
                case "instant": this.instantThreshold = Double.parseDouble(value); break;
                case "digest": this.digestThreshold = Double.parseDouble(value); break;
                case "archive": this.archiveThreshold = Double.parseDouble(value); break;
                case "musthave": this.mustHaveKeywords = List.of(value.split(",")); break;
                case "location": this.preferredLocation = value; break;
                case "remote": this.allowRemoteSearch = Boolean.parseBoolean(value); break;
                default: return "❌ Nieznany parametr: " + param;
            }
            log.info("🔄 Parametr {} zaktualizowany dynamicznie na: {}", param, value);
            return "✅ Parametr " + param + " ustawiony na: " + value;
        } catch (Exception e) {
            log.error("❌ Błąd aktualizacji parametru {}: {}", param, e.getMessage());
            return "❌ Błąd aktualizacji: " + e.getMessage();
        }
    }

    public String getCurrentStatus() {
        return """
                ⚙️ **Aktualna Konfiguracja:**
                - Semantic Threshold (AI Start): %s%%
                - Instant Threshold (Alert): %s
                - Digest Threshold (Daily): %s
                - Archive Threshold (Min): %s
                - Must-Have Keywords: %s
                - Preferred Location: %s
                - Allow Remote Search: %s
                """.formatted(semanticThreshold, instantThreshold, digestThreshold, archiveThreshold, mustHaveKeywords, preferredLocation, allowRemoteSearch);
    }
}
