package com.workdone.backend.profile.parser;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Używam LLM-a, żeby z "ściany tekstu" wyciągniętej z PDF-a zrobił porządny profil JSON.
 * Dzięki temu wiem, jakie mam skille, ile lat expa i gdzie w ogóle szukam roboty.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CvSemanticParser {

    private final ChatClient.Builder builder;
    private ChatClient chatClient;

    @PostConstruct
    public void init() {
        this.chatClient = builder.build();
    }

    public CvProfileResult parse(String cvText) {
        log.info("Zaczynam semantyczną analizę CV przez AI");
        return chatClient.prompt()
                .user(u -> u.text("""
                    Analyze the following CV and return a structured JSON profile.
                    Focus on extracting technical skills and candidate preferences.

                    Return strict JSON:
                    {
                      "skills": ["skill1", "skill2"],
                      "experienceYears": number,
                      "seniority": "string",
                      "topKeywords": ["java", "spring", "sql"],
                      "location": "City, Country or 'Poland' or 'Remote'"
                    }
                    
                    The "topKeywords" should contain 3-5 most important technologies from the CV.
                    The "location" should be the candidate's current city or region found in CV.

                    CV Content:
                    """ + cvText))
                .call()
                .entity(CvProfileResult.class);
    }

    public record CvProfileResult(
            List<String> skills,
            int experienceYears,
            String seniority,
            List<String> topKeywords, // Najważniejsze tagi, które będą użyte do szukania ofert
            String location          // Moja lokalizacja - żeby system wiedział czy mam dojazd
    ) {}
}
