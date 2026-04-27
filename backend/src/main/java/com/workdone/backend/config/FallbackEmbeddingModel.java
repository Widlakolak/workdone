package com.workdone.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@Primary
public class FallbackEmbeddingModel implements EmbeddingModel {

    private static final int LOCAL_FALLBACK_DIMENSIONS = 1024;

    private final EmbeddingModel cohereModel;
    private final EmbeddingModel openAiModel;
    private final EmbeddingModel localModel;
    private final ThreadLocal<AtomicBoolean> localFallbackUsed = ThreadLocal.withInitial(() -> new AtomicBoolean(false));

    public FallbackEmbeddingModel(
            @Qualifier("cohereAiEmbeddingModel") EmbeddingModel cohereModel,
            @Qualifier("openAiEmbeddingModel") EmbeddingModel openAiModel,
            @Qualifier("localHashEmbeddingModel") EmbeddingModel localModel) {
        this.cohereModel = cohereModel;
        this.openAiModel = openAiModel;
        this.localModel = localModel;
    }

    @Override
    public float[] embed(String text) {
        try {
            float[] result = cohereModel.embed(text);
            markLocalFallbackUsed(false);
            return result;
        } catch (Exception e1) {
            log.warn("⚠️ Cohere (String) zawiódł, próbuję OpenAI...");
            try {
                float[] result = openAiModel.embed(text);
                markLocalFallbackUsed(false);
                return result;
            } catch (Exception e2) {
                log.warn("⚠️ OpenAI (String) zawiódł, próbuję Local Bean...");
                try {
                    float[] result = localModel.embed(text);
                    markLocalFallbackUsed(true);
                    return result;
                } catch (Exception e3) {
                    log.error("❌ Wszystkie modele (String) zawiodły. Używam hardcoded localEmbed.");
                    markLocalFallbackUsed(true);
                    return localEmbed(text);
                }
            }
        }
    }

    @Override
    public float[] embed(Document document) {
        try {
            float[] result = cohereModel.embed(document);
            markLocalFallbackUsed(false);
            return result;
        } catch (Exception e1) {
            log.warn("⚠️ Cohere (Document) zawiódł, przechodzę na OpenAI...");
            try {
                float[] result = openAiModel.embed(document);
                markLocalFallbackUsed(false);
                return result;
            } catch (Exception e2) {
                log.warn("⚠️ OpenAI (Document) zawiódł, przechodzę na Local Bean...");
                markLocalFallbackUsed(true);
                return localModel.embed(document);
            }
        }
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        try {
            List<float[]> result = cohereModel.embed(texts);
            markLocalFallbackUsed(false);
            return result;
        } catch (Exception e1) {
            log.warn("⚠️ Cohere (List) zawiódł, próbuję OpenAI...");
            try {
                List<float[]> result = openAiModel.embed(texts);
                markLocalFallbackUsed(false);
                return result;
            } catch (Exception e2) {
                log.warn("⚠️ OpenAI (List) zawiódł, próbuję Local Bean...");
                try {
                    markLocalFallbackUsed(true);
                    return localModel.embed(texts);
                } catch (Exception e3) {
                    log.error("❌ Fallback (List) zawiódł całkowicie.");
                    markLocalFallbackUsed(true);
                    return texts.stream().map(this::localEmbed).toList();
                }
            }
        }
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        try {
            EmbeddingResponse response = cohereModel.call(request);
            markLocalFallbackUsed(false);
            return response;
        } catch (Exception e1) {
            log.warn("⚠️ Cohere (call) zawiódł, próbuję OpenAI...");
            try {
                EmbeddingResponse response = openAiModel.call(request);
                markLocalFallbackUsed(false);
                return response;
            } catch (Exception e2) {
                log.warn("⚠️ OpenAI (call) zawiódł, próbuję Local Bean...");
                markLocalFallbackUsed(true);
                return localModel.call(request);
            }
        }
    }

    @Override
    public int dimensions() {
        return LOCAL_FALLBACK_DIMENSIONS;
    }

    public boolean usedLocalFallbackInCurrentThread() {
        return localFallbackUsed.get().get();
    }

    private float[] localEmbed(String text) {
        float[] vector = new float[LOCAL_FALLBACK_DIMENSIONS];
        if (text == null || text.isBlank()) {
            return vector;
        }

        String normalized = text.toLowerCase(Locale.ROOT);
        String[] tokens = normalized.split("\\s+");
        for (String token : tokens) {
            byte[] bytes = token.getBytes(StandardCharsets.UTF_8);
            int hash = Arrays.hashCode(bytes);
            int index = Math.floorMod(hash, LOCAL_FALLBACK_DIMENSIONS);
            vector[index] += 1.0f;
        }

        float norm = 0f;
        for (float value : vector) {
            norm += value * value;
        }
        norm = (float) Math.sqrt(norm);
        if (norm == 0f) {
            return vector;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / norm;
        }
        return vector;
    }

    private void markLocalFallbackUsed(boolean value) {
        localFallbackUsed.get().set(value);
    }
}