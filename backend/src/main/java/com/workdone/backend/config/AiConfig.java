package com.workdone.backend.config;

import com.cohere.api.Cohere;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Slf4j
@Configuration
public class AiConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @Primary
    @Qualifier("groqChatModel")
    public ChatModel groqChatModel(
            @Value("${spring.ai.groq.api-key}") String apiKey,
            @Value("${spring.ai.groq.base-url}") String baseUrl,
            @Value("${spring.ai.groq.model}") String model) {

        var api = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .build())
                .build();
    }

    @Bean
    @Qualifier("googleGenAiChatModel")
    public ChatModel googleGenAiChatModel(GoogleGenAiChatModel.Builder builder) {
        return builder.build();
    }

    @Bean
    @Qualifier("openAiChatModel")
    public ChatModel openAiChatModel(OpenAiChatModel.Builder builder) {
        return builder.build();
    }

    @Bean
    @Qualifier("openAiEmbeddingModel")
    public OpenAiEmbeddingModel openAiEmbeddingModel(
            @Value("${spring.ai.openai.api-key}") String apiKey) {

        var api = OpenAiApi.builder()
                .apiKey(apiKey)
                .build();

        // Ustawiamy model i wymiary (1024), aby pasowały do bazy i Cohere
        var options = OpenAiEmbeddingOptions.builder()
                .model("text-embedding-3-small")
                .dimensions(1024)
                .build();

        return new OpenAiEmbeddingModel(api, MetadataMode.EMBED, options);
    }

    @Bean
    public Cohere cohereClient(@Value("${spring.ai.cohere.api-key}") String apiKey) {
        return Cohere.builder()
                .token(apiKey)
                .clientName("workdone-backend")
                .build();
    }
}