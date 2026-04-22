package com.workdone.backend.analysis;

import com.workdone.backend.model.JobOfferRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
public class OfferEmbeddingService {

    private final EmbeddingModel cohereModel;
    private final EmbeddingModel openAiModel;

    public OfferEmbeddingService(
            @Qualifier("fallbackEmbeddingModel") EmbeddingModel cohereModel,
            @Qualifier("openAiEmbeddingModel") EmbeddingModel openAiModel) {
        this.cohereModel = cohereModel;
        this.openAiModel = openAiModel;
    }

    public float[] embed(String text) {
        try {
            log.debug("🔍 Próbuję wygenerować embedding przez Cohere...");
            return cohereModel.embed(text);
        } catch (Exception e) {
            log.warn("⚠️ Błąd Cohere, próbuję fallback na OpenAI: {}", e.getMessage());
            try {
                return openAiModel.embed(text);
            } catch (Exception ex) {
                log.error("❌ Oba modele embeddingu zawiodły!");
                throw ex;
            }
        }
    }

    public float[] embedOffer(JobOfferRecord offer) {
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

        return embed(input);
    }

    private static String defaultText(String value) {
        return value == null ? "" : value;
    }
}
