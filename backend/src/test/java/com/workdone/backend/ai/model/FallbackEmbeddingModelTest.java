package com.workdone.backend.ai.model;

import com.workdone.backend.ai.embedding.FallbackEmbeddingModel;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FallbackEmbeddingModelTest {

    @Test
    void shouldUseOpenAiFallbackWhenPrimaryEmbeddingFails() {
        EmbeddingModel cohereMock = mock(EmbeddingModel.class);
        EmbeddingModel openaiMock = mock(EmbeddingModel.class);
        EmbeddingModel localMock = mock(EmbeddingModel.class);

        when(cohereMock.embed("java spring")).thenThrow(new RuntimeException("cohere down"));
        when(openaiMock.embed("java spring")).thenReturn(new float[] {1.0f, 2.0f});

        FallbackEmbeddingModel model = new FallbackEmbeddingModel(Optional.of(cohereMock), Optional.of(openaiMock), localMock);
        float[] result = model.embed("java spring");

        assertThat(result).containsExactly(1.0f, 2.0f);
        Mockito.verify(openaiMock).embed("java spring");
        assertThat(model.usedLocalFallbackInCurrentThread()).isFalse(); // Bo OpenAI zadziałało
    }

    @Test
    void shouldUseLocalFallbackWhenBothPrimaryAndSecondaryFail() {
        EmbeddingModel cohereMock = mock(EmbeddingModel.class);
        EmbeddingModel openaiMock = mock(EmbeddingModel.class);
        EmbeddingModel localMock = mock(EmbeddingModel.class);

        when(cohereMock.embed("java spring")).thenThrow(new RuntimeException("cohere down"));
        when(openaiMock.embed("java spring")).thenThrow(new RuntimeException("openai quota"));
        when(localMock.embed("java spring")).thenReturn(new float[] {3.0f, 4.0f});

        FallbackEmbeddingModel model = new FallbackEmbeddingModel(Optional.of(cohereMock), Optional.of(openaiMock), localMock);
        float[] result = model.embed("java spring");

        assertThat(result).containsExactly(3.0f, 4.0f);
        Mockito.verify(localMock).embed("java spring");
        assertThat(model.usedLocalFallbackInCurrentThread()).isTrue();
    }

    @Test
    void shouldUseHardcodedLocalEmbedWhenAllModelsFail() {
        EmbeddingModel cohereMock = mock(EmbeddingModel.class);
        EmbeddingModel openaiMock = mock(EmbeddingModel.class);
        EmbeddingModel localMock = mock(EmbeddingModel.class);

        when(cohereMock.embed(any(String.class))).thenThrow(new RuntimeException("error"));
        when(openaiMock.embed(any(String.class))).thenThrow(new RuntimeException("error"));
        when(localMock.embed(any(String.class))).thenThrow(new RuntimeException("error"));

        FallbackEmbeddingModel model = new FallbackEmbeddingModel(Optional.of(cohereMock), Optional.of(openaiMock), localMock);
        float[] result = model.embed("java spring boot");

        assertThat(result).hasSize(1024);
        assertThat(model.usedLocalFallbackInCurrentThread()).isTrue();
    }

    @Test
    void shouldUseLocalFallbackForBatchWhenBothProvidersFail() {
        EmbeddingModel cohereMock = mock(EmbeddingModel.class);
        EmbeddingModel openaiMock = mock(EmbeddingModel.class);
        EmbeddingModel localMock = mock(EmbeddingModel.class);
        List<String> batch = List.of("java", "kotlin");

        when(cohereMock.embed(batch)).thenThrow(new RuntimeException("error"));
        when(openaiMock.embed(batch)).thenThrow(new RuntimeException("error"));
        when(localMock.embed(batch)).thenThrow(new RuntimeException("error"));

        FallbackEmbeddingModel model = new FallbackEmbeddingModel(Optional.of(cohereMock), Optional.of(openaiMock), localMock);
        List<float[]> result = model.embed(batch);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).hasSize(1024);
        assertThat(model.usedLocalFallbackInCurrentThread()).isTrue();
    }

    @Test
    void shouldHandleMissingCohereAndOpenAiBeans() {
        EmbeddingModel localMock = mock(EmbeddingModel.class);
        when(localMock.embed("test")).thenReturn(new float[]{0.1f, 0.2f});

        FallbackEmbeddingModel model = new FallbackEmbeddingModel(Optional.empty(), Optional.empty(), localMock);
        float[] result = model.embed("test");

        assertThat(result).containsExactly(0.1f, 0.2f);
        Mockito.verify(localMock).embed("test");
        assertThat(model.usedLocalFallbackInCurrentThread()).isTrue();
    }
}
