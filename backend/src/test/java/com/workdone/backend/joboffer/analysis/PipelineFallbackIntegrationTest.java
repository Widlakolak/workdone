package com.workdone.backend.joboffer.analysis;

import com.workdone.backend.common.config.WorkDoneProperties;
import com.workdone.backend.common.util.OfferContentBuilder;
import com.workdone.backend.joboffer.analysis.DynamicConfigService;
import com.workdone.backend.joboffer.analysis.OfferEmbeddingService;
import com.workdone.backend.joboffer.analysis.OfferScoringService;
import com.workdone.backend.common.model.JobOfferRecord;
import com.workdone.backend.joboffer.notification.DiscordNotifier;
import com.workdone.backend.profile.service.CandidateProfileService;
import com.workdone.backend.ai.storage.EmbeddingCacheStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
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
    @Mock private DynamicConfigService dynamicConfigService;
    @Mock private CandidateProfileService candidateProfileService;
    @Mock private WorkDoneProperties properties;
    @Mock private DiscordNotifier discordNotifier;
    @Mock private OfferContentBuilder contentBuilder;
    @Mock private EmbeddingCacheStore embeddingCache;

    @BeforeEach
    void setUp() {
        scoringService = new OfferScoringService(
                groqModel, openAiModel, geminiModel, properties,
                dynamicConfigService, candidateProfileService, discordNotifier
        );
        scoringService.init();

        // Teraz OfferEmbeddingService przyjmuje jeden model (zazwyczaj fallbackowy) i builder
        embeddingService = new OfferEmbeddingService(cohereModel, contentBuilder, embeddingCache);
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
        verify(discordNotifier, atLeastOnce()).sendAiAlert(anyString());
    }

    @Test
    void shouldReturnEmbeddingFromModel() {
        // GIVEN
        float[] expected = new float[]{0.5f};
        when(cohereModel.embed(anyString())).thenReturn(expected);

        // WHEN
        float[] result = embeddingService.embed("test");

        // THEN
        assertThat(result).isEqualTo(expected);
        verify(cohereModel).embed("test");
    }
}
