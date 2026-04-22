package com.workdone.backend.profile.service;

import com.workdone.backend.config.WorkDoneProperties;
import com.workdone.backend.profile.parser.CvDocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * Ten serwis przetrząsa mój folder z CV i łączy wszystko w jedną wielką "ścianę tekstu".
 * Przydatne, jak mam osobne CV po polsku, angielsku i np. profil z LinkedIna w PDF.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CvAggregationService {

    private final WorkDoneProperties properties;
    private final CvDocumentParser parser;

    public String buildMergedProfile() {
        Path basePath = Path.of(properties.profile().inputDirectory());

        if (Files.notExists(basePath)) {
            log.warn("⚠️ Folder z CV zniknął albo go nie ma: {}", basePath);
            return "";
        }

        try {
            log.info("🔍 Zbieram i parsuję wszystkie pliki z: {}", basePath);
            return Files.walk(basePath)
                    .filter(Files::isRegularFile)
                    .map(path -> {
                        try {
                            // Każdy plik zamieniam na tekst
                            return parser.extractText(path);
                        } catch (Exception e) {
                            log.error("❌ Coś poszło nie tak przy czytaniu CV: {}", path, e);
                            return "[PARSE_ERROR] " + path.getFileName();
                        }
                    })
                    .collect(Collectors.joining("\n\n")); // Łączę teksty, oddzielając je pustymi liniami
        } catch (IOException e) {
            log.error("❌ Nie mogę się dobrać do katalogu z CV", e);
            throw new RuntimeException("Nie można odczytać katalogu CV", e);
        }
    }
}
