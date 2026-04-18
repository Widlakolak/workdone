package com.workdone.backend.profile.parser;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class CvSemanticParser {

    private final ChatClient chatClient;

    public CvSemanticParser(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String parse(String cvText) {
        return chatClient.prompt()
                .user(u -> u.text("""
                    Extract structured candidate profile.

                    Return JSON:
                    {
                      skills: [],
                      experience: number,
                      seniority: string
                    }

                    CV:
                    """ + cvText))
                .call()
                .content();
    }
}