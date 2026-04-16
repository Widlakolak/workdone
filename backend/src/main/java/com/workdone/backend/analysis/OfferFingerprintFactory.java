package com.workdone.backend.analysis;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class OfferFingerprintFactory {

    public String createFrom(String title, String company, String location) {
        String normalized = normalize(title) + "|" + normalize(company) + "|" + normalize(location);
        return sha256(normalized);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String ascii = Normalizer.normalize(replaceUnmappableLetters(value), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String normalized = ascii
                .replaceAll("[^\\p{Alnum}]+", " ")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
        return normalized;
    }

    private String replaceUnmappableLetters(String value) {
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
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Brak algorytmu SHA-256", ex);
        }
    }
}