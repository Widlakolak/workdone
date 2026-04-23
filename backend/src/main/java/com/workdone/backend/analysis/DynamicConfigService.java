package com.workdone.backend.analysis;

import com.workdone.backend.config.WorkDoneProperties;
import com.workdone.backend.model.LocationPolicy;
import com.workdone.backend.profile.service.CandidateProfileService;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private List<LocationPolicy> locationPolicies = new ArrayList<>();
    private boolean allowRemoteSearch;
    private boolean bestOfferFallbackEnabled;
    private String preferredSeniority;

    public DynamicConfigService(WorkDoneProperties properties, @Lazy CandidateProfileService profileService) {
        this.properties = properties;
        this.profileService = profileService;
    }

    @PostConstruct
    public void init() {
        this.semanticThreshold = properties.matching() != null ? properties.matching().semanticThreshold() : 0.7;
        this.instantThreshold = properties.matching() != null ? properties.matching().instantThreshold() : 0.8;
        this.digestThreshold = properties.matching() != null ? properties.matching().digestThreshold() : 0.6;
        this.archiveThreshold = properties.matching() != null ? properties.matching().archiveThreshold() : 0.4;
        this.mustHaveKeywords = new ArrayList<>(properties.matching() != null ? properties.matching().mustHaveKeywords() : List.of());
        this.preferredSeniority = "junior";
        this.allowRemoteSearch = properties.search() == null || properties.search().allowRemote();
        this.bestOfferFallbackEnabled = false;

        // Domyślna lokalizacja z propertiesów jako pierwsza polityka (akceptujemy wszystko)
        String defaultLoc = properties.search() != null ? properties.search().defaultLocation() : "Poland";
        this.locationPolicies.add(new LocationPolicy(defaultLoc, true, true, true, 5));

        log.info("⚙️ Konfiguracja dynamiczna zainicjalizowana: Semantic={}%, Policies={}, Remote={}, Seniority={}", 
                semanticThreshold, locationPolicies.size(), allowRemoteSearch, preferredSeniority);
    }

    public void syncWithProfile() {
        String profileLocation = profileService.getLocation();
        if (profileLocation != null && !profileLocation.isBlank()) {
            log.info("🔄 Synchronizacja danych z profilu CV: {}, {}", profileLocation, profileService.getSeniority());
            // Jeśli profil ma nową lokalizację, dodajemy ją jako preferowaną (jeśli jeszcze nie ma)
            if (locationPolicies.stream().noneMatch(p -> p.city().equalsIgnoreCase(profileLocation))) {
                this.locationPolicies.add(new LocationPolicy(profileLocation, true, true, true, 5));
            }
            this.preferredSeniority = profileService.getSeniority();
        }
    }

    public String updateConfig(String param, String value) {
        try {
            switch (param.toLowerCase()) {
                case "semantic": this.semanticThreshold = Double.parseDouble(value); break;
                case "instant": this.instantThreshold = Double.parseDouble(value); break;
                case "digest": this.digestThreshold = Double.parseDouble(value); break;
                case "archive": this.archiveThreshold = Double.parseDouble(value); break;
                case "musthave":
                    this.mustHaveKeywords = List.of(value.split(",")).stream()
                            .map(String::trim)
                            .filter(keyword -> !keyword.isBlank())
                            .toList();
                    break;
                case "location": parseAndAddLocation(value); break;
                case "remote": this.allowRemoteSearch = Boolean.parseBoolean(value); break;
                case "best_offer_fallback": this.bestOfferFallbackEnabled = Boolean.parseBoolean(value); break;
                case "seniority": this.preferredSeniority = value; break;
                case "clear_locations": this.locationPolicies.clear(); break;
                default: return "❌ Nieznany parametr: " + param;
            }
            log.info("🔄 Parametr {} zaktualizowany dynamicznie.", param);
            return "✅ Parametr " + param + " zaktualizowany.";
        } catch (Exception e) {
            log.error("❌ Błąd aktualizacji parametru {}: {}", param, e.getMessage());
            return "❌ Błąd aktualizacji: " + e.getMessage();
        }
    }

    private void parseAndAddLocation(String value) {
        // Format: city:remote:hybrid:onsite:days (np. Lodz:true:true:true:2)
        String[] parts = value.split(":");
        if (parts.length >= 4) {
            String city = parts[0];
            boolean r = Boolean.parseBoolean(parts[1]);
            boolean h = Boolean.parseBoolean(parts[2]);
            boolean o = Boolean.parseBoolean(parts[3]);
            Integer days = parts.length > 4 ? Integer.parseInt(parts[4]) : null;
            
            // Usuwamy starą politykę dla tego miasta, jeśli istnieje
            locationPolicies.removeIf(p -> p.city().equalsIgnoreCase(city));
            locationPolicies.add(new LocationPolicy(city, r, h, o, days));
        }
    }

    public String getCurrentStatus() {
        String policies = locationPolicies.stream()
                .map(p -> "- %s (R:%s, H:%s, O:%s, MaxDays:%s)".formatted(p.city(), p.allowRemote(), p.allowHybrid(), p.allowOnsite(), p.maxDaysInOffice()))
                .collect(Collectors.joining("\n"));

        return """
                ⚙️ **Aktualna Konfiguracja:**
                - Semantic Threshold: %s%%
                - Must-Have Keywords: %s
                - Allow Remote Search: %s
                - Best Offer Fallback: %s
                - Preferred Seniority: %s
                - Location Policies:
                %s
                ""\".formatted(semanticThreshold, mustHaveKeywords, allowRemoteSearch, bestOfferFallbackEnabled, preferredSeniority, policies);
                """.formatted(semanticThreshold, mustHaveKeywords, allowRemoteSearch, preferredSeniority, policies);
    }
}
