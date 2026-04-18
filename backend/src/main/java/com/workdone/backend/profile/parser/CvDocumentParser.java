package com.workdone.backend.profile.parser;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class CvDocumentParser {

    private final Tika tika = new Tika();

    public String extractText(Path file) throws IOException {
        if (Files.size(file) > 10_000_000) { // max 10MB
            throw new RuntimeException("Plik jest za duży");
        }
        try {
            return tika.parseToString(file);
        } catch (IOException | TikaException e) {
            throw new RuntimeException("Nie udało się przeanalizować CV: " + file, e);
        }
    }
}