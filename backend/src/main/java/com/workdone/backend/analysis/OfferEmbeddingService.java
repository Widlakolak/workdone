package com.workdone.backend.analysis;

import com.workdone.backend.model.JobOfferRecord;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@ConditionalOnBean(EmbeddingModel.class)
public class OfferEmbeddingService {

    private final EmbeddingModel embeddingModel;

    public OfferEmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public float[] embed(JobOfferRecord offer) {
        String input = """
                title: %s
                company: %s
                location: %s
                tech: %s
                description: %s
                """.formatted(
                defaultText(offer.title()),
                defaultText(offer.companyName()),
                defaultText(offer.location()),
                Objects.toString(offer.techStack(), "[]"),
                defaultText(offer.rawDescription())
        );

        return embeddingModel.embed(input);
    }

    private static String defaultText(String value) {
        return value == null ? "" : value;
    }
}