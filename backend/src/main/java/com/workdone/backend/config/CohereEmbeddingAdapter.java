package com.workdone.backend.config;

import com.cohere.api.Cohere;
import com.cohere.api.resources.v2.requests.V2EmbedRequest;
import com.cohere.api.types.EmbedInputType;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component("cohereAiEmbeddingModel")
@Qualifier("cohereAiEmbeddingModel")
public class CohereEmbeddingAdapter implements EmbeddingModel {

    private final Cohere cohere;

    public CohereEmbeddingAdapter(Cohere cohere) {
        this.cohere = cohere;
    }

    @Override
    public float[] embed(String text) {
        // Używam modelu wielojęzycznego od Cohere, bo świetnie radzi sobie z miksem 
        // polskiego i angielskiego w ogłoszeniach IT.
        var response = cohere.v2().embed(V2EmbedRequest.builder()
                .model("embed-multilingual-v3.0")
                .inputType(EmbedInputType.SEARCH_DOCUMENT)
                .texts(List.of(text))
                .build());

        return response.getEmbeddings().getFloat()
                .map(list -> {
                    List<Double> embeddings = list.get(0);
                    float[] result = new float[embeddings.size()];
                    for (int i = 0; i < embeddings.size(); i++) {
                        result[i] = embeddings.get(i).floatValue();
                    }
                    return result;
                })
                .orElseThrow(() -> new RuntimeException("Cohere returned no embeddings"));
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getText());
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        return texts.stream().map(this::embed).toList();
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        // Implementacja wymaganego interfejsu Spring AI dla embeddingów
        List<Embedding> embeddings = new ArrayList<>();
        List<String> instructions = request.getInstructions();
        for (int i = 0; i < instructions.size(); i++) {
            float[] vector = embed(instructions.get(i));
            embeddings.add(new Embedding(vector, i));
        }
        return new EmbeddingResponse(embeddings);
    }
}
