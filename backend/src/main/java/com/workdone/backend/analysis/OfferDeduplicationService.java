package com.workdone.backend.analysis;

import com.workdone.backend.storage.OfferVectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferDeduplicationService {

    private final OfferVectorStore vectorStore;

    public boolean isDuplicate(float[] embedding) {
        if (embedding == null) {
            return false;
        }

        // Wywołujemy nową metodę w VectorStore, która przyjmuje float[]
        // Dzięki temu nie płacimy drugi raz za embedding tej samej treści
        List<String> similarIds = vectorStore.findSimilarOfferIds(embedding, 0.95, 1);
        boolean isDuplicate = !similarIds.isEmpty();
        
        if (isDuplicate) {
            log.info("🚫 Wykryto duplikat oferty w Vector Store.");
        }

        return isDuplicate;
    }
}
