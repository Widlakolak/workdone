package com.workdone.backend.profile;

import com.workdone.backend.config.WorkDoneProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Service
public class CandidateProfileService {

    private static final Logger log = LoggerFactory.getLogger(CandidateProfileService.class);

    private final WorkDoneProperties properties;

    public CandidateProfileService(WorkDoneProperties properties) {
        this.properties = properties;
    }

    public String profileContext() {
        Path basePath = Path.of(properties.profile().inputDirectory());
        if (Files.notExists(basePath)) {
            log.warn("Brak katalogu profilu: {}", basePath);
            return "";
        }

        try (Stream<Path> stream = Files.walk(basePath, 2)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.naturalOrder())
                    .limit(20)
                    .toList();

            StringBuilder buffer = new StringBuilder();
            for (Path file : files) {
                buffer.append(readText(file));
                buffer.append("\n");
            }
            return buffer.toString();
        } catch (IOException ex) {
            log.warn("Nie udało się odczytać katalogu profilu {}", basePath, ex);
            return "";
        }
    }

    private String readText(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        try {
            if (name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".json")) {
                return Files.readString(file);
            }
            return "[OCR_PENDING] " + file.getFileName();
        } catch (IOException ex) {
            log.debug("Pominięto plik {}", file, ex);
            return "";
        }
    }
}