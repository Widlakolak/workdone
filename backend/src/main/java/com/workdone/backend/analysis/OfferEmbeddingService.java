package com.workdone.backend.analysis;

import com.workdone.backend.format.OfferContentBuilder;
import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.storage.JdbcEmbeddingCacheStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class OfferEmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final OfferContentBuilder contentBuilder;
    private final JdbcEmbeddingCacheStore cache;

    public OfferEmbeddingService(
            @Qualifier("fallbackEmbeddingModel") EmbeddingModel embeddingModel,
            OfferContentBuilder contentBuilder,
            JdbcEmbeddingCacheStore cache) {
        this.embeddingModel = embeddingModel;
        this.contentBuilder = contentBuilder;
        this.cache = cache;
    }

    public float[] embed(String text) {
        if (text == null || text.isBlank()) return new float[1024];
        String hash = generateHash(text);

        // 1. Sprawdź Cache
        Optional<float[]> cached = cache.get(hash);
        if (cached.isPresent()) return cached.get();

        // 2. Jeśli brak, pobierz i zapisz
        try {
            float[] vector = embeddingModel.embed(text);
            cache.put(hash, vector);
            return vector;
        } catch (Exception e) {
            handleError(e);
            throw e;
        }
    }

    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) return List.of();

        List<float[]> results = new ArrayList<>(texts.size());
        List<String> missingTexts = new ArrayList<>();
        List<Integer> missingIndices = new ArrayList<>();

        // Sprawdzamy co mamy w DB
        for (int i = 0; i < texts.size(); i++) {
            results.add(null); // placeholder
            String hash = generateHash(texts.get(i));
            Optional<float[]> cached = cache.get(hash);

            if (cached.isPresent()) {
                results.set(i, cached.get());
            } else {
                missingTexts.add(texts.get(i));
                missingIndices.add(i);
            }
        }

        // Pobieramy z API tylko brakujące (oszczędność)
        if (!missingTexts.isEmpty()) {
            log.info("🚀 Cache miss: Pobieram {} nowych embeddingów (z {} tekstów)...", missingTexts.size(), texts.size());
            try {
                List<float[]> newVectors = embeddingModel.embed(missingTexts);
                for (int i = 0; i < missingTexts.size(); i++) {
                    float[] v = newVectors.get(i);
                    cache.put(generateHash(missingTexts.get(i)), v);
                    results.set(missingIndices.get(i), v);
                }
            } catch (Exception e) {
                handleError(e);
                throw e;
            }
        }
        return results;
    }

    public List<float[]> embedOffers(List<JobOfferRecord> offers) {
        List<String> inputs = offers.stream().map(contentBuilder::buildTechnicalContent).toList();
        return embedBatch(inputs);
    }

    public float[] embedOffer(JobOfferRecord offer) {
        return embed(contentBuilder.buildTechnicalContent(offer));
    }

    private String generateHash(String text) {
        return DigestUtils.md5DigestAsHex(text.getBytes(StandardCharsets.UTF_8));
    }

    private void handleError(Exception e) {
        if (e.getMessage().contains("429") || e.getMessage().toLowerCase().contains("rate limit")) {
            log.warn("🚨 [RATE LIMIT] Przekroczono limit zapytań embeddingu (429).");
        } else {
            log.error("❌ [EMBEDDING ERROR] {}", e.getMessage());
        }
    }
}