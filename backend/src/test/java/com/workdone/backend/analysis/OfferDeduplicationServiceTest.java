package com.workdone.backend.analysis;

import com.workdone.backend.config.FallbackEmbeddingModel;
import com.workdone.backend.storage.OfferVectorStore;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OfferDeduplicationServiceTest {

    @Test
    void shouldSkipVectorDeduplicationWhenLocalFallbackEmbeddingIsActive() {
        OfferVectorStore vectorStore = Mockito.mock(OfferVectorStore.class);
        FallbackEmbeddingModel fallbackEmbeddingModel = Mockito.mock(FallbackEmbeddingModel.class);
        when(fallbackEmbeddingModel.usedLocalFallbackInCurrentThread()).thenReturn(true);

        OfferDeduplicationService service = new OfferDeduplicationService(vectorStore, fallbackEmbeddingModel);

        boolean result = service.isDuplicate(new float[]{0.1f, 0.2f});

        assertThat(result).isFalse();
        verify(vectorStore, never()).findSimilarOfferIds(Mockito.any(float[].class), Mockito.anyDouble(), Mockito.anyInt());
    }

    @Test
    void shouldUseVectorStoreWhenRemoteEmbeddingIsActive() {
        OfferVectorStore vectorStore = Mockito.mock(OfferVectorStore.class);
        FallbackEmbeddingModel fallbackEmbeddingModel = Mockito.mock(FallbackEmbeddingModel.class);
        when(fallbackEmbeddingModel.usedLocalFallbackInCurrentThread()).thenReturn(false);
        when(vectorStore.findSimilarOfferIds(Mockito.any(float[].class), Mockito.eq(0.95), Mockito.eq(1)))
                .thenReturn(List.of("offer-1"));

        OfferDeduplicationService service = new OfferDeduplicationService(vectorStore, fallbackEmbeddingModel);

        boolean result = service.isDuplicate(new float[]{0.3f, 0.4f});

        assertThat(result).isTrue();
    }
}