package com.workdone.backend.analysis;

import com.workdone.backend.format.OfferContentBuilder;
import com.workdone.backend.model.JobOfferRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class OfferEmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final OfferContentBuilder contentBuilder;

    public OfferEmbeddingService(
            @Qualifier("fallbackEmbeddingModel") EmbeddingModel embeddingModel,
            OfferContentBuilder contentBuilder) {
        this.embeddingModel = embeddingModel;
        this.contentBuilder = contentBuilder;
    }

    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }

    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) return List.of();
        log.info("🚀 Generuję embeddingi dla paczki {} tekstów...", texts.size());
        return embeddingModel.embed(texts);
    }

    public List<float[]> embedOffers(List<JobOfferRecord> offers) {
        List<String> inputs = offers.stream()
                .map(contentBuilder::buildTechnicalContent)
                .toList();
        return embedBatch(inputs);
    }

    public float[] embedOffer(JobOfferRecord offer) {
        String input = contentBuilder.buildTechnicalContent(offer);
        return embed(input);
    }
}
