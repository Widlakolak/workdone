package com.workdone.backend.profile.service;

import com.workdone.backend.config.WorkDoneProperties;
import com.workdone.backend.profile.parser.CvDocumentParser;
import com.workdone.backend.profile.parser.CvSemanticParser;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

@Service
public class CvAggregationService {

    private final WorkDoneProperties properties;
    private final CvDocumentParser parser;
    private final CvSemanticParser semanticParser;

    public CvAggregationService(WorkDoneProperties properties,
                                CvDocumentParser parser,
                                CvSemanticParser semanticParser) {
        this.properties = properties;
        this.parser = parser;
        this.semanticParser = semanticParser;
    }

    public String buildMergedProfile() {
        Path basePath = Path.of(properties.profile().inputDirectory());

        if (Files.notExists(basePath)) {
            return "";
        }

        try {
            String merged = Files.walk(basePath)
                    .filter(Files::isRegularFile)
                    .map(path -> {
                        try {
                            return parser.extractText(path);
                        } catch (Exception e) {
                            return "[PARSE_ERROR] " + path.getFileName();
                        }
                    })
                    .collect(Collectors.joining("\n\n"));

            String structured = semanticParser.parse(merged);

            System.out.println("=== STRUCTURED CV ===");
            System.out.println(structured);

            return merged;
        } catch (IOException e) {
            throw new RuntimeException("Cannot read CV directory", e);
        }
    }
}