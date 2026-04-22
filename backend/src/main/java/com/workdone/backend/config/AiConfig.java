package com.workdone.backend.config;

import com.cohere.api.Cohere;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;

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
        
        // Groq jest kompatybilny z API OpenAI, więc używam OpenAiApi do połączenia
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

        return new OpenAiEmbeddingModel(api);
    }

    @Bean
    public Cohere cohereClient(@Value("${spring.ai.cohere.api-key}") String apiKey) {
        return Cohere.builder()
                .token(apiKey)
                .clientName("workdone-backend")
                .build();
    }

    @Bean
    @Primary
    @Qualifier("fallbackEmbeddingModel")
    public EmbeddingModel fallbackEmbeddingModel(
            @Qualifier("cohereAiEmbeddingModel") EmbeddingModel primaryModel,
            @Qualifier("openAiEmbeddingModel") EmbeddingModel fallbackModel) {
        // Dekorator dla modeli embeddingu: jak Cohere (tańsze/lepsze do tekstu) padnie, 
        // to automatycznie przełączam się na OpenAI, żeby system nie stanął.
        return new EmbeddingModel() {
            @Override
            public float[] embed(String text) {
                try {
                    return primaryModel.embed(text);
                } catch (Exception e) {
                    return fallbackModel.embed(text);
                }
            }

            @Override
            public float[] embed(Document document) {
                try {
                    return primaryModel.embed(document);
                } catch (Exception e) {
                    return fallbackModel.embed(document);
                }
            }

            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                try {
                    return primaryModel.call(request);
                } catch (Exception e) {
                    return fallbackModel.call(request);
                }
            }
        };
    }
}
