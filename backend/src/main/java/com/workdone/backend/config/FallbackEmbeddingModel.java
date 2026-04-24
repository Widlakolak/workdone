package com.workdone.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@Primary
public class FallbackEmbeddingModel implements EmbeddingModel {

    private static final int LOCAL_FALLBACK_DIMENSIONS = 1024;

    private final EmbeddingModel primaryModel;
    private final EmbeddingModel fallbackModel;
    private final ThreadLocal<AtomicBoolean> localFallbackUsed = ThreadLocal.withInitial(() -> new AtomicBoolean(false));

    public FallbackEmbeddingModel(
            @Qualifier("cohereAiEmbeddingModel") EmbeddingModel primaryModel,
            @Qualifier("openAiEmbeddingModel") EmbeddingModel fallbackModel) {
        this.primaryModel = primaryModel;
        this.fallbackModel = fallbackModel;
    }

    @Override
    public float[] embed(String text) {
        try {
            float[] result = primaryModel.embed(text);
            markLocalFallbackUsed(false);
            return result;
        } catch (Exception e) {
            log.warn("⚠️ Główny model embeddingu (String) zawiódł ({}), próbuję fallback...", e.getMessage());
            try {
                float[] result = fallbackModel.embed(text);
                markLocalFallbackUsed(false);
                return result;
            } catch (Exception fallbackException) {
                log.error("❌ Fallback embeddingu (String) też zawiódł ({}). Używam lokalnego embeddingu awaryjnego.",
                        fallbackException.getMessage());
                markLocalFallbackUsed(true);
                return localEmbed(text);
            }
        }
    }

    @Override
    public float[] embed(Document document) {
        try {
            float[] result = primaryModel.embed(document);
            markLocalFallbackUsed(false);
            return result;
        } catch (Exception e) {
            log.warn("⚠️ Główny model embeddingu (Document) zawiódł ({}), próbuję fallback...", e.getMessage());
            float[] result = fallbackModel.embed(document);
            markLocalFallbackUsed(false);
            return result;
        }
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        try {
            List<float[]> result = primaryModel.embed(texts);
            markLocalFallbackUsed(false);
            return result;
        } catch (Exception e) {
            log.warn("⚠️ Główny model embeddingu (List) zawiódł ({}), próbuję fallback...", e.getMessage());
            try {
                List<float[]> result = fallbackModel.embed(texts);
                markLocalFallbackUsed(false);
                return result;
            } catch (Exception fallbackException) {
                log.error("❌ Fallback embeddingu (List) też zawiódł ({}). Używam lokalnych embeddingów awaryjnych.",
                        fallbackException.getMessage());
                markLocalFallbackUsed(true);
                return texts.stream().map(this::localEmbed).toList();
            }
        }
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        try {
            EmbeddingResponse response = primaryModel.call(request);
            markLocalFallbackUsed(false);
            return response;
        } catch (Exception e) {
            log.warn("⚠️ Główny model embeddingu (call) zawiódł ({}), próbuję fallback...", e.getMessage());
            try {
                EmbeddingResponse response = fallbackModel.call(request);
                markLocalFallbackUsed(false);
                return response;
            } catch (Exception fallbackException) {
                log.error("❌ Fallback embeddingu (call) też zawiódł ({}). Przechodzę na lokalne embeddingi awaryjne.",
                        fallbackException.getMessage());
                markLocalFallbackUsed(true);
                List<String> instructions = request.getInstructions();
                if (instructions == null || instructions.isEmpty()) {
                    return new EmbeddingResponse(List.of());
                }
                List<Embedding> embeddings = new ArrayList<>();

                for (int i = 0; i < instructions.size(); i++) {
                    embeddings.add(new Embedding(localEmbed(instructions.get(i)), i));
                }

                return new EmbeddingResponse(embeddings);
            }
        }
    }

    @Override
    public int dimensions() {
        return primaryModel.dimensions();
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