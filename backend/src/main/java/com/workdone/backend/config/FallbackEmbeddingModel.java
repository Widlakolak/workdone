package com.workdone.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class FallbackEmbeddingModel implements EmbeddingModel {

    private final EmbeddingModel primaryModel;
    private final EmbeddingModel fallbackModel;

    public FallbackEmbeddingModel(
            @Qualifier("cohereAiEmbeddingModel") EmbeddingModel primaryModel,
            @Qualifier("openAiEmbeddingModel") EmbeddingModel fallbackModel) {
        this.primaryModel = primaryModel;
        this.fallbackModel = fallbackModel;
    }

    @Override
    public float[] embed(String text) {
        try {
            return primaryModel.embed(text);
        } catch (Exception e) {
            log.warn("⚠️ Główny model embeddingu (String) zawiódł ({}), próbuję fallback...", e.getMessage());
            return fallbackModel.embed(text);
        }
    }

    @Override
    public float[] embed(Document document) {
        try {
            return primaryModel.embed(document);
        } catch (Exception e) {
            log.warn("⚠️ Główny model embeddingu (Document) zawiódł ({}), próbuję fallback...", e.getMessage());
            return fallbackModel.embed(document);
        }
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        try {
            return primaryModel.embed(texts);
        } catch (Exception e) {
            log.warn("⚠️ Główny model embeddingu (List) zawiódł ({}), próbuję fallback...", e.getMessage());
            return fallbackModel.embed(texts);
        }
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        try {
            return primaryModel.call(request);
        } catch (Exception e) {
            log.warn("⚠️ Główny model embeddingu (call) zawiódł ({}), próbuję fallback...", e.getMessage());
            return fallbackModel.call(request);
        }
    }

    @Override
    public int dimensions() {
        return primaryModel.dimensions();
    }
}
