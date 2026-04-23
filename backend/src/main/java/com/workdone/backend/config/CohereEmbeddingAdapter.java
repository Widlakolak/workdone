package com.workdone.backend.config;

import com.cohere.api.Cohere;
import com.cohere.api.resources.v2.requests.V2EmbedRequest;
import com.cohere.api.types.EmbedInputType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component("cohereAiEmbeddingModel")
@Qualifier("cohereAiEmbeddingModel")
public class CohereEmbeddingAdapter implements EmbeddingModel {

    private final Cohere cohere;

    public CohereEmbeddingAdapter(Cohere cohere) {
        this.cohere = cohere;
    }

    @Override
    public float[] embed(String text) {
        List<float[]> results = embed(List.of(text));
        return results.get(0);
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getText());
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) return List.of();
        
        log.debug("🚀 Cohere Batch: Wysyłam {} tekstów w jednym zapytaniu (limit RPM: 5)", texts.size());
        
        var response = cohere.v2().embed(V2EmbedRequest.builder()
                .model("embed-multilingual-v3.0")
                .inputType(EmbedInputType.SEARCH_DOCUMENT)
                .texts(texts)
                .build());

        return response.getEmbeddings().getFloat()
                .map(list -> {
                    List<float[]> results = new ArrayList<>();
                    for (List<Double> embedding : list) {
                        float[] vector = new float[embedding.size()];
                        for (int i = 0; i < embedding.size(); i++) {
                            vector[i] = embedding.get(i).floatValue();
                        }
                        results.add(vector);
                    }
                    return results;
                })
                .orElseThrow(() -> new RuntimeException("Cohere returned no embeddings"));
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> instructions = request.getInstructions();
        List<float[]> vectors = embed(instructions);
        
        List<Embedding> embeddings = new ArrayList<>();
        for (int i = 0; i < vectors.size(); i++) {
            embeddings.add(new Embedding(vectors.get(i), i));
        }
        return new EmbeddingResponse(embeddings);
    }
}
