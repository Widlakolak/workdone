package com.workdone.backend.analysis;

import com.workdone.backend.storage.OfferVectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OfferDeduplicationService {

    private final OfferVectorStore vectorStore;

    public OfferDeduplicationService(OfferVectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public boolean isDuplicate(String content) {

        List<String> similar = vectorStore.findSimilarOfferIds(
                content,
                0.92, // threshold
                3     // topK
        );

        return !similar.isEmpty();
    }
}