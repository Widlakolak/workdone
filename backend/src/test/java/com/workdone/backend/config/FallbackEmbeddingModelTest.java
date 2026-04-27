package com.workdone.backend.config;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FallbackEmbeddingModelTest {

    @Test
    void shouldUseOpenAiFallbackWhenPrimaryEmbeddingFails() {
        EmbeddingModel cohere = mock(EmbeddingModel.class);
        EmbeddingModel openai = mock(EmbeddingModel.class);
        EmbeddingModel local = mock(EmbeddingModel.class);

        when(cohere.embed("java spring")).thenThrow(new RuntimeException("cohere down"));
        when(openai.embed("java spring")).thenReturn(new float[] {1.0f, 2.0f});

        FallbackEmbeddingModel model = new FallbackEmbeddingModel(cohere, openai, local);
        float[] result = model.embed("java spring");

        assertThat(result).containsExactly(1.0f, 2.0f);
        Mockito.verify(openai).embed("java spring");
        assertThat(model.usedLocalFallbackInCurrentThread()).isFalse(); // Bo OpenAI zadziałało
    }

    @Test
    void shouldUseLocalFallbackWhenBothPrimaryAndSecondaryFail() {
        EmbeddingModel cohere = mock(EmbeddingModel.class);
        EmbeddingModel openai = mock(EmbeddingModel.class);
        EmbeddingModel local = mock(EmbeddingModel.class);

        when(cohere.embed("java spring")).thenThrow(new RuntimeException("cohere down"));
        when(openai.embed("java spring")).thenThrow(new RuntimeException("openai quota"));
        when(local.embed("java spring")).thenReturn(new float[] {3.0f, 4.0f});

        FallbackEmbeddingModel model = new FallbackEmbeddingModel(cohere, openai, local);
        float[] result = model.embed("java spring");

        assertThat(result).containsExactly(3.0f, 4.0f);
        Mockito.verify(local).embed("java spring");
        assertThat(model.usedLocalFallbackInCurrentThread()).isTrue();
    }

    @Test
    void shouldUseHardcodedLocalEmbedWhenAllModelsFail() {
        EmbeddingModel cohere = mock(EmbeddingModel.class);
        EmbeddingModel openai = mock(EmbeddingModel.class);
        EmbeddingModel local = mock(EmbeddingModel.class);

        when(cohere.embed(any(String.class))).thenThrow(new RuntimeException("error"));
        when(openai.embed(any(String.class))).thenThrow(new RuntimeException("error"));
        when(local.embed(any(String.class))).thenThrow(new RuntimeException("error"));

        FallbackEmbeddingModel model = new FallbackEmbeddingModel(cohere, openai, local);
        float[] result = model.embed("java spring boot");

        assertThat(result).hasSize(1024);
        assertThat(model.usedLocalFallbackInCurrentThread()).isTrue();
    }

    @Test
    void shouldUseLocalFallbackForBatchWhenBothProvidersFail() {
        EmbeddingModel cohere = mock(EmbeddingModel.class);
        EmbeddingModel openai = mock(EmbeddingModel.class);
        EmbeddingModel local = mock(EmbeddingModel.class);
        List<String> batch = List.of("java", "kotlin");

        when(cohere.embed(batch)).thenThrow(new RuntimeException("error"));
        when(openai.embed(batch)).thenThrow(new RuntimeException("error"));
        when(local.embed(batch)).thenThrow(new RuntimeException("error"));

        FallbackEmbeddingModel model = new FallbackEmbeddingModel(cohere, openai, local);
        List<float[]> result = model.embed(batch);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).hasSize(1024);
        assertThat(model.usedLocalFallbackInCurrentThread()).isTrue();
    }
}