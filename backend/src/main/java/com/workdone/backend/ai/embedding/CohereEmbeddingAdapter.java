package com.workdone.backend.ai.embedding;

import com.cohere.api.Cohere;
import com.cohere.api.resources.v2.requests.V2EmbedRequest;
import com.cohere.api.types.EmbedInputType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component("cohereAiEmbeddingModel")
@Qualifier("cohereAiEmbeddingModel")
@ConditionalOnBean(Cohere.class)
public class CohereEmbeddingAdapter implements EmbeddingModel {

    private static final int DEFAULT_MAX_BATCH_SIZE = 96;

    private final CohereBatchClient batchClient;
    private final int maxBatchSize;
    private final CohereRateLimiter rateLimiter;
    private final String model;

    @Autowired
    public CohereEmbeddingAdapter(
            Cohere cohere,
            @Value("${workdone.ai.embedding.cohere.model:embed-multilingual-v3.0}") String model,
            @Value("${workdone.ai.cohere.max-batch-size:96}") int maxBatchSize,
            @Value("${workdone.ai.cohere.max-requests-per-minute:5}") int maxRequestsPerMinute
    ) {
        this(new CohereBatchClient() {
            @Override
            public List<float[]> embed(List<String> texts) {
                var response = cohere.v2().embed(V2EmbedRequest.builder()
                        .model(model)
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
        }, model, maxBatchSize, maxRequestsPerMinute, new ThreadSleeper());
    }

    public CohereEmbeddingAdapter(CohereBatchClient batchClient, String model, int maxBatchSize, int maxRequestsPerMinute, Sleeper sleeper) {
        this.batchClient = batchClient;
        this.model = model;
        this.maxBatchSize = Math.max(1, Math.min(maxBatchSize, DEFAULT_MAX_BATCH_SIZE));
        this.rateLimiter = new CohereRateLimiter(maxRequestsPerMinute, sleeper);
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

        List<float[]> results = new ArrayList<>(texts.size());
        for (int from = 0; from < texts.size(); from += maxBatchSize) {
            int to = Math.min(from + maxBatchSize, texts.size());
            List<String> batch = texts.subList(from, to);

            rateLimiter.awaitPermit();
            log.debug("🚀 Cohere Batch: wysyłam teksty {}-{} z {} (batch: {}, maxRPM: {})",
                    from + 1, to, texts.size(), batch.size(), rateLimiter.maxRequestsPerMinute());

            List<float[]> batchResult = batchClient.embed(batch);
            if (batchResult.size() != batch.size()) {
                throw new IllegalStateException("Cohere returned %d embeddings for batch size %d".formatted(batchResult.size(), batch.size()));
            }
            results.addAll(batchResult);
        }

        return results;
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

    public interface CohereBatchClient {
        List<float[]> embed(List<String> texts);
    }

    public interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    public static class ThreadSleeper implements Sleeper {
        @Override
        public void sleep(long millis) throws InterruptedException {
            Thread.sleep(millis);
        }
    }

    public static class CohereRateLimiter {

        private final long minIntervalMillis;
        private final int maxRequestsPerMinute;
        private final Sleeper sleeper;

        private long lastRequestEpochMillis = 0L;

        public CohereRateLimiter(int maxRequestsPerMinute, Sleeper sleeper) {
            this.maxRequestsPerMinute = Math.max(1, maxRequestsPerMinute);
            this.minIntervalMillis = Math.max(1L, 60_000L / this.maxRequestsPerMinute);
            this.sleeper = sleeper;
        }

        public synchronized void awaitPermit() {
            long now = System.currentTimeMillis();
            long earliestNextRequest = lastRequestEpochMillis + minIntervalMillis;
            long waitMillis = earliestNextRequest - now;

            if (waitMillis > 0) {
                try {
                    sleeper.sleep(waitMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for Cohere rate limit window", e);
                }
            }

            lastRequestEpochMillis = System.currentTimeMillis();
        }

        public int maxRequestsPerMinute() {
            return maxRequestsPerMinute;
        }
    }
}
