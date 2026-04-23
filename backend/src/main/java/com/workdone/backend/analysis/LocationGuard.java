package com.workdone.backend.analysis;

import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.model.LocationPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strażnik lokalizacji. Decyduje, czy w ogóle chcemy patrzeć na tę ofertę,
 * bazując na dynamicznej konfiguracji polityk lokalizacyjnych.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationGuard {

    private final DynamicConfigService dynamicConfig;

    // Regex do wyciągania liczby dni z biura z opisu oferty
    private static final Pattern DAYS_IN_OFFICE_PATTERN = Pattern.compile("(\\d+)\\s*(?:dni|day|days|raz|times)\\s*(?:w tygodniu|a week|per week)");

    public boolean isAccepted(JobOfferRecord offer) {
        String normalizedLocation = normalize(offer.location());
        String normalizedDescription = normalize(offer.rawDescription());
        String fullText = normalizedLocation + " " + normalizedDescription;

        // 1. Sprawdzamy, czy oferta jest zdalna/hybrydowa na podstawie tekstu
        boolean isOfferRemote = isRemote(fullText);
        boolean isOfferHybrid = isHybrid(fullText);
        boolean isOfferOnsite = !isOfferRemote && !isOfferHybrid; // Uznajemy, że jeśli nie zdalna/hybrydowa, to stacjonarna

        // 2. Szukamy polityki dla konkretnego miasta z oferty
        Optional<LocationPolicy> matchingPolicy = dynamicConfig.getLocationPolicies().stream()
                .filter(policy -> normalizedLocation.contains(normalize(policy.city())))
                .findFirst();

        if (matchingPolicy.isPresent()) {
            LocationPolicy policy = matchingPolicy.get();
            
            // Jeśli oferta jest zdalna i polityka na to pozwala
            if (isOfferRemote && policy.allowRemote()) {
                log.debug("✅ Akceptacja zdalna dla miasta {}: Oferta zdalna, polityka pozwala.", policy.city());
                return true;
            }
            // Jeśli oferta jest hybrydowa i polityka na to pozwala
            if (isOfferHybrid && policy.allowHybrid()) {
                if (policy.maxDaysInOffice() != null) {
                    int offerDaysInOffice = extractDaysInOffice(fullText);
                    if (offerDaysInOffice == -1 || offerDaysInOffice <= policy.maxDaysInOffice()) {
                        log.debug("✅ Akceptacja hybrydowa dla miasta {}: Oferta hybrydowa, polityka pozwala (dni: {} <= {}).", policy.city(), offerDaysInOffice, policy.maxDaysInOffice());
                        return true;
                    } else {
                        log.debug("❌ Odrzucenie hybrydowe dla miasta {}: Oferta hybrydowa, ale za dużo dni w biurze ({} > {}).", policy.city(), offerDaysInOffice, policy.maxDaysInOffice());
                        return false;
                    }
                }
                log.debug("✅ Akceptacja hybrydowa dla miasta {}: Oferta hybrydowa, polityka pozwala (bez limitu dni).", policy.city());
                return true;
            }
            // Jeśli oferta jest stacjonarna i polityka na to pozwala
            if (isOfferOnsite && policy.allowOnsite()) {
                log.debug("✅ Akceptacja stacjonarna dla miasta {}: Oferta stacjonarna, polityka pozwala.", policy.city());
                return true;
            }
            log.debug("❌ Odrzucenie dla miasta {}: Brak dopasowania trybu pracy (R:{}, H:{}, O:{}).", policy.city(), isOfferRemote, isOfferHybrid, isOfferOnsite);
            return false; // Polityka dla miasta istnieje, ale nie pasuje tryb pracy
        }

        // 3. Jeśli nie ma konkretnej polityki dla miasta, sprawdzamy ogólne ustawienia
        if (isOfferRemote && dynamicConfig.isAllowRemoteSearch()) {
            log.debug("✅ Akceptacja ogólna: Oferta zdalna, globalne ustawienie pozwala na zdalną.");
            return true;
        }

        log.debug("❌ Odrzucenie: Brak pasującej polityki lokalizacyjnej i nie jest to oferta zdalna z globalnym pozwoleniem.");
        return false;
    }

    // Metody pomocnicze do sprawdzania trybu pracy
    private boolean isRemote(String text) {
        return text.contains("remote") || text.contains("anywhere") || text.contains("100% remote") || text.contains("zdalna");
    }

    private boolean isHybrid(String text) {
        return text.contains("hybrid") || text.contains("hybrydowa");
    }

    private int extractDaysInOffice(String text) {
        Matcher matcher = DAYS_IN_OFFICE_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                log.warn("Nie udało się sparsować liczby dni z biura: {}", matcher.group(1));
            }
        }
        return -1; // -1 oznacza, że nie znaleziono lub błąd parsowania
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
