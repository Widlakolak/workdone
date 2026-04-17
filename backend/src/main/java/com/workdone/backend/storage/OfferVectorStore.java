package com.workdone.backend.storage;

import com.workdone.backend.model.JobOfferRecord;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.springframework.ai.vectorstore.SearchRequest;

import java.util.List;
import java.util.Map;

@Component
public class OfferVectorStore {

    private final VectorStore vectorStore;

    public OfferVectorStore(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void save(JobOfferRecord offer, String content) {

        Document doc = new Document(
                content,
                Map.of(
                        "offerId", offer.id(),
                        "company", offer.companyName(),
                        "source", offer.sourcePlatform()
                )
        );

        vectorStore.add(List.of(doc));
    }

    public List<String> findSimilarOfferIds(String content, double threshold, int topK) {

        SearchRequest request = SearchRequest.builder()
                .query(content)
                .topK(topK)
                .similarityThreshold(threshold)
                .build();

        return vectorStore.similaritySearch(request).stream()
                .map(doc -> (String) doc.getMetadata().get("offerId"))
                .toList();
    }
}