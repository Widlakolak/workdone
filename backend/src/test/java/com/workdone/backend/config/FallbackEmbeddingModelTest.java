package com.workdone.backend.config;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

class FallbackEmbeddingModelTest {

    @Test
    void shouldUseOpenAiFallbackWhenPrimaryEmbeddingFails() {
        EmbeddingModel primaryModel = Mockito.mock(EmbeddingModel.class);
        EmbeddingModel fallbackModel = Mockito.mock(EmbeddingModel.class);

        Mockito.when(primaryModel.embed("java spring")).thenThrow(new RuntimeException("cohere down"));
        Mockito.when(fallbackModel.embed("java spring")).thenReturn(new float[] {1.0f, 2.0f});

        FallbackEmbeddingModel model = new FallbackEmbeddingModel(primaryModel, fallbackModel);
        float[] result = model.embed("java spring");

        assertThat(result).containsExactly(1.0f, 2.0f);
        Mockito.verify(fallbackModel).embed("java spring");
    }

    @Test
    void shouldUseOpenAiFallbackForBatchEmbeddingWhenPrimaryFails() {
        EmbeddingModel primaryModel = Mockito.mock(EmbeddingModel.class);
        EmbeddingModel fallbackModel = Mockito.mock(EmbeddingModel.class);
        List<String> batch = List.of("oferta 1", "oferta 2");

        Mockito.when(primaryModel.embed(batch)).thenThrow(new RuntimeException("cohere timeout"));
        Mockito.when(fallbackModel.embed(batch)).thenReturn(List.of(new float[] {0.1f}, new float[] {0.2f}));

        FallbackEmbeddingModel model = new FallbackEmbeddingModel(primaryModel, fallbackModel);
        List<float[]> result = model.embed(batch);

        assertThat(result).hasSize(2);
        Mockito.verify(fallbackModel).embed(batch);
    }

    @Test
    void shouldUseLocalFallbackWhenBothProvidersFailForSingleText() {
        EmbeddingModel primaryModel = Mockito.mock(EmbeddingModel.class);
        EmbeddingModel fallbackModel = Mockito.mock(EmbeddingModel.class);

        Mockito.when(primaryModel.embed(any(String.class))).thenThrow(new RuntimeException("cohere down"));
        Mockito.when(fallbackModel.embed(any(String.class))).thenThrow(new RuntimeException("openai quota"));

        FallbackEmbeddingModel model = new FallbackEmbeddingModel(primaryModel, fallbackModel);
        float[] result = model.embed("java spring boot");

        assertThat(result).hasSize(1024);
        assertThat(result).isNotNull();
        boolean hasPositiveValue = false;
        for (float value : result) {
            if (value > 0f) {
                hasPositiveValue = true;
                break;
            }
        }
        assertThat(hasPositiveValue).isTrue();
    }

    @Test
    void shouldUseLocalFallbackWhenBothProvidersFailForBatch() {
        EmbeddingModel primaryModel = Mockito.mock(EmbeddingModel.class);
        EmbeddingModel fallbackModel = Mockito.mock(EmbeddingModel.class);
        List<String> batch = List.of("oferta java", "oferta kotlin");

        Mockito.when(primaryModel.embed(batch)).thenThrow(new RuntimeException("cohere timeout"));
        Mockito.when(fallbackModel.embed(batch)).thenThrow(new RuntimeException("openai quota"));

        FallbackEmbeddingModel model = new FallbackEmbeddingModel(primaryModel, fallbackModel);
        List<float[]> result = model.embed(batch);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).hasSize(1024);
        assertThat(result.get(1)).hasSize(1024);
    }
}