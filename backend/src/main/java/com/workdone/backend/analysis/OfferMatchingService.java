package com.workdone.backend.analysis;

import com.workdone.backend.model.JobOfferRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferMatchingService {

    private final MustHaveGroupConfig groupConfig;
    private final DynamicConfigService dynamicConfig;

    public boolean passesMustHave(JobOfferRecord offer) {
        String context = (offer.title() + " " + offer.rawDescription() + " " + offer.techStack())
                .toLowerCase(Locale.ROOT);

        // Najpierw sprawdzam moje własne słowa kluczowe (te, które ręcznie wrzuciłem przez bota na Discordzie)
        List<String> dynamicMustHave = dynamicConfig.getMustHaveKeywords();
        if (dynamicMustHave != null && !dynamicMustHave.isEmpty()) {
            boolean matchesDynamic = dynamicMustHave.stream()
                    .anyMatch(k -> context.contains(normalize(k)));
            if (!matchesDynamic) {
                log.debug("❌ Oferta {} odrzucona: brak moich słów kluczowych z bota", offer.title());
                return false;
            }
        }

        // Teraz sprawdzam grupy technologiczne - zarówno te stałe, jak i te wyciągnięte z mojego CV
        List<MustHaveGroup> groups = groupConfig.groups();
        int matchedGroups = 0;

        for (MustHaveGroup group : groups) {
            boolean matched = containsAny(context, group.keywords());
            if (matched) {
                matchedGroups++;
                log.debug("✅ Grupa {} dopasowana dla {}", group.name(), offer.title());
            } else if (group.required()) {
                // Jeśli brakuje czegoś krytycznego (np. Javy), to od razu odrzucam
                log.debug("❌ Oferta {} odrzucona: brak wymaganej grupy {}", offer.title(), group.name());
                return false;
            }
        }

        // Sprawdzam, czy oferta przeszła wystarczającą liczbę grup (żeby nie brać byle czego)
        int minRequired = groupConfig.minGroupsToPass();
        boolean passed = matchedGroups >= minRequired;
        
        if (!passed) {
            log.debug("❌ Oferta {} odrzucona: dopasowano {}/{} grup", offer.title(), matchedGroups, minRequired);
        }
        
        return passed;
    }

    private boolean containsAny(String text, List<String> keywords) {
        return keywords.stream()
                .anyMatch(k -> text.contains(normalize(k)));
    }

    private String normalize(String s) {
        return s.toLowerCase().replace("-", "").replace(" ", "");
    }
}
