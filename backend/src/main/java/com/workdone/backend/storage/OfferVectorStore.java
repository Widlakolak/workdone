package com.workdone.backend.storage;

import com.workdone.backend.model.JobOfferRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Mój most do bazy wektorowej (np. ChromaDB). 
 * Pozwala na zapisywanie ofert w formie wektorów i szukanie semantyczne.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OfferVectorStore {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Wrzucam ofertę do bazy wektorowej. 
     * Dzięki temu mogę potem szybko sprawdzić, czy inna oferta nie jest "zbyt podobna".
     */
    public void save(JobOfferRecord offer, String content) {
        log.debug("Zapisywanie oferty w Vector Store: {}", offer.id());
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

    /**
     * Szukam ofert podobnych do podanego tekstu. 
     * Wykorzystuję to do de-duplikacji (np. czy ta sama oferta nie wisi na 5 portalach).
     */
    public List<String> findSimilarOfferIds(String content, double threshold, int topK) {
        log.debug("🔍 Przeszukiwanie wektorowe (szukam duplikatów dla treści)");
        
        SearchRequest request = SearchRequest.builder()
                .query(content)
                .topK(topK)
                .similarityThreshold(threshold)
                .build();

        List<Document> results = vectorStore.similaritySearch(request);
        return results.stream()
                .map(doc -> (String) doc.getMetadata().get("offerId"))
                .toList();
    }

    /**
     * Szukam podobnych ofert używając już wygenerowanego embeddingu.
     * Pozwala to uniknąć ponownego kosztu generowania wektora przez API.
     */
    public List<String> findSimilarOfferIds(float[] embedding, double threshold, int topK) {
        log.debug("🔍 Przeszukiwanie wektorowe (używam gotowego embeddingu)");

        String sql = """
             SELECT metadata->>'offerId' FROM vector_store 
             WHERE 1 - (embedding <=> ?::vector) >= ? 
             ORDER BY embedding <=> ?::vector ASC 
             LIMIT ?
             """;

        return jdbcTemplate.queryForList(sql, String.class,
                embedding, threshold, embedding, topK);
    }

    /**
     * Liczę podobieństwo cosinusowe "ręcznie". 
     * To mi służy do szybkiego porównania oferty z moim CV bez odpytywania bazy wektorowej.
     */
    public double calculateCosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        double denominator = (Math.sqrt(normA) * Math.sqrt(normB));
        return denominator == 0 ? 0.0 : dotProduct / denominator;
    }
}
