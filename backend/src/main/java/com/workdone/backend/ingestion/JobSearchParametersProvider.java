package com.workdone.backend.ingestion;

import com.workdone.backend.analysis.DynamicConfigService;
import com.workdone.backend.analysis.MustHaveGroup;
import com.workdone.backend.analysis.MustHaveGroupConfig;
import com.workdone.backend.profile.service.CandidateProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobSearchParametersProvider {

    private final CandidateProfileService profileService;
    private final DynamicConfigService dynamicConfig;
    private final MustHaveGroupConfig mustHaveGroupConfig;

    public SearchContext getContext() {
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

        return SearchContext.builder()
                .keywords(finalKeywords)
                .location(dynamicConfig.getPreferredLocation())
                .remoteOnly(dynamicConfig.isAllowRemoteSearch())
                .maxResults(100)
                .build();
    }
}
