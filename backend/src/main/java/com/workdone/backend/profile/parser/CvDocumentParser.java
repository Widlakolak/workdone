package com.workdone.backend.profile.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Wyciągam tekst z plików (PDF, Word) za pomocą Apache Tika. 
 * Robię to, zanim wyślę cokolwiek do AI, żeby nie płacić za mielenie pustych danych.
 */
@Slf4j
@Service
public class CvDocumentParser {

    private final Tika tika = new Tika();

    public String extractText(Path file) throws IOException {
        log.debug("Parsing document: {}", file.getFileName());
        
        // Limituję plik do 10MB, żeby system nie spuchł przy jakichś gigantycznych skanach
        if (Files.size(file) > 10_000_000) { 
            log.error("Plik {} jest za wielki! ({} bajtów)", file.getFileName(), Files.size(file));
            throw new RuntimeException("Plik jest za duży");
        }
        
        try {
            // Tika sama zgadnie format i wyciągnie czysty tekst
            return tika.parseToString(file);
        } catch (IOException | TikaException e) {
            log.error("Failed to parse document: {}", file, e);
            throw new RuntimeException("Nie udało się przeanalizować CV: " + file, e);
        }
    }
}
