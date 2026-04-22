package com.workdone.backend.analysis;

import com.workdone.backend.config.WorkDoneProperties;
import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.profile.service.CandidateProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineFallbackIntegrationTest {

    private OfferScoringService scoringService;
    private OfferEmbeddingService embeddingService;

    @Mock private ChatModel groqModel;
    @Mock private ChatModel openAiModel;
    @Mock private ChatModel geminiModel;
    @Mock private EmbeddingModel cohereModel;
    @Mock private EmbeddingModel openAiEmbeddingModel;
    @Mock private DynamicConfigService dynamicConfigService;
    @Mock private CandidateProfileService candidateProfileService;
    @Mock private WorkDoneProperties properties;

    @BeforeEach
    void setUp() {
        scoringService = new OfferScoringService(
                groqModel, openAiModel, geminiModel, properties,
                dynamicConfigService, candidateProfileService
        );
        scoringService.init();

        // Inicjalizujemy serwis z oboma modelami, żeby sprawdzić fallback
        embeddingService = new OfferEmbeddingService(cohereModel, openAiEmbeddingModel);
    }

    @Test
    void shouldFallbackToOpenAiWhenGroqFails() {
        when(groqModel.call(any(Prompt.class))).thenThrow(new RuntimeException("Groq error"));
        when(openAiModel.call(any(Prompt.class))).thenThrow(new RuntimeException("OpenAI error"));

        try {
            scoringService.score(JobOfferRecord.builder().title("Java").sourceUrl("http://t.com").build());
        } catch (Exception e) {
            // Success: went through chain
        }

        verify(groqModel, atLeastOnce()).call(any(Prompt.class));
        verify(openAiModel, atLeastOnce()).call(any(Prompt.class));
    }

    @Test
    void shouldFallbackToOpenAiEmbeddingWhenCohereFails() {
        // GIVEN
        when(cohereModel.embed(anyString())).thenThrow(new RuntimeException("Cohere error"));
        float[] expected = new float[]{0.5f};
        when(openAiEmbeddingModel.embed(anyString())).thenReturn(expected);

        // WHEN
        float[] result = embeddingService.embed("test");

        // THEN
        assertThat(result).isEqualTo(expected);
        verify(cohereModel).embed("test");
        verify(openAiEmbeddingModel).embed("test");
    }
}
