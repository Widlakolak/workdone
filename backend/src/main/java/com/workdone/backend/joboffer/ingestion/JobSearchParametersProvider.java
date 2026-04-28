package com.workdone.backend.joboffer.ingestion;

import com.workdone.backend.joboffer.analysis.DynamicConfigService;
import com.workdone.backend.joboffer.analysis.MustHaveGroup;
import com.workdone.backend.joboffer.analysis.MustHaveGroupConfig;
import com.workdone.backend.common.model.LocationPolicy;
import com.workdone.backend.profile.service.CandidateProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobSearchParametersProvider {

    private final CandidateProfileService profileService;
    private final DynamicConfigService dynamicConfig;
    private final MustHaveGroupConfig mustHaveGroupConfig;

    public List<SearchContext> getContexts() {
        // Składam listę słów kluczowych do wyszukiwarek ofert
        Set<String> keywords = new LinkedHashSet<>();
        
        // Moje priorytety z Discorda idą na pierwszy ogień
        keywords.addAll(dynamicConfig.getMustHaveKeywords());
        
        // Dorzucam to, co AI wyłapało jako ważne w moim CV
        keywords.addAll(profileService.getSuggestedKeywords());

        // Na koniec dorzucam główne technologie (np. Java, Spring), żeby zawęzić wyniki
        mustHaveGroupConfig.groups().stream()
                .filter(MustHaveGroup::required)
                .flatMap(g -> g.keywords().stream().limit(2)) // 2 pierwsze z grupy wystarczą, żeby nie przesadzić
                .forEach(keywords::add);

        // Wywalam śmieci i duplikaty
        List<String> finalKeywords = keywords.stream()
                .filter(k -> k != null && k.length() > 1)
                .map(String::toLowerCase)
                .distinct()
                .limit(10) // 10 słów to max, inaczej API wyszukiwarek zgłupieje albo mnie zablokuje
                .toList();

        if (finalKeywords.isEmpty()) {
            // Jakimś cudem nic nie mam? Szukam Javy, bo to moja baza
            log.warn("⚠️ Brak słów kluczowych do wyszukiwania! Używam 'Java' jako fallback.");
            finalKeywords = List.of("java");
        }

        List<SearchContext> searchContexts = new ArrayList<>();
        int maxResults = 100; // Domyślny limit wyników na jedno zapytanie

        boolean globalRemotePolicyExists = false;

        for (LocationPolicy policy : dynamicConfig.getLocationPolicies()) {
            // Kontekst dla pracy zdalnej w ramach danej polityki
            if (policy.allowRemote()) {
                searchContexts.add(SearchContext.builder()
                        .keywords(finalKeywords)
                        .location(policy.city())
                        .remoteOnly(true)
                        .maxResults(maxResults)
                        .build());
                if (policy.city() == null || policy.city().equalsIgnoreCase("remote") || policy.city().equalsIgnoreCase("anywhere")) {
                    globalRemotePolicyExists = true;
                }
            }
            // Kontekst dla pracy hybrydowej/stacjonarnej w ramach danej polityki
            if (policy.allowHybrid() || policy.allowOnsite()) {
                searchContexts.add(SearchContext.builder()
                        .keywords(finalKeywords)
                        .location(policy.city())
                        .remoteOnly(false) // remoteOnly = false oznacza, że szukamy ofert stacjonarnych/hybrydowych
                        .maxResults(maxResults)
                        .build());
            }
        }

        // Jeśli globalne wyszukiwanie zdalne jest włączone, a nie ma polityki, która by je pokrywała
        if (dynamicConfig.isAllowRemoteSearch() && !globalRemotePolicyExists) {
            log.info("🌐 Dodaję globalny kontekst wyszukiwania zdalnego, ponieważ allowRemoteSearch jest true i brak dedykowanej polityki.");
            searchContexts.add(SearchContext.builder()
                    .keywords(finalKeywords)
                    .location(SearchContext.REMOTE_GLOBAL)
                    .remoteOnly(true)
                    .maxResults(maxResults)
                    .build());
        }
        
        if (searchContexts.isEmpty()) {
            log.warn("⚠️ Brak kontekstów wyszukiwania! Dodaję domyślny kontekst dla Javy w Polsce (zdalnie).");
            searchContexts.add(SearchContext.builder()
                    .keywords(List.of("java"))
                    .location("Poland")
                    .remoteOnly(true)
                    .maxResults(maxResults)
                    .build());
        }

        return searchContexts;
    }
}
