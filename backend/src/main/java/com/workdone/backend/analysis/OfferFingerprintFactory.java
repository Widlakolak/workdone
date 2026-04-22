package com.workdone.backend.analysis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;

@Slf4j
@Component
public class OfferFingerprintFactory {

    /**
     * Tworzę unikalny skrót (fingerprint) oferty na podstawie tytułu, firmy i lokalizacji.
     * Dzięki temu wiem, że to ta sama oferta, nawet jeśli zmienił się jej URL na portalu.
     */
    public String createFrom(String title, String company, String location) {
        String normalized = normalize(title) + "|" + normalize(company) + "|" + normalize(location);
        String fingerprint = sha256(normalized);
        log.trace("Generated fingerprint {} for {} | {} | {}", fingerprint, title, company, location);
        return fingerprint;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        // Wywalam polskie znaki, znaki specjalne i nadmiarowe spacje, żeby porównanie było odporne na literówki
        String ascii = Normalizer.normalize(replaceUnmappableLetters(value), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return ascii
                .replaceAll("[^\\p{Alnum}]+", " ")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
    }

    private String replaceUnmappableLetters(String value) {
        // Specyficzne znaki, których standardowy Normalizer NFD nie zawsze łapie poprawnie
        return value
                .replace('ł', 'l')
                .replace('Ł', 'L')
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .replace('ø', 'o')
                .replace('Ø', 'O');
    }

    private String sha256(String value) {
        try {
            // Liczę SHA-256 z połączonych i znormalizowanych pól
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            log.error("SHA-256 algorithm not found", ex);
            throw new IllegalStateException("Brak algorytmu SHA-256", ex);
        }
    }
}
