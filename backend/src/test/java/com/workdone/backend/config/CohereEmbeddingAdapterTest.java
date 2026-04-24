package com.workdone.backend.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CohereEmbeddingAdapterTest {

    @Test
    void shouldSplitIntoBatchesOfAtMost96() {
        List<Integer> observedBatchSizes = new ArrayList<>();

        CohereEmbeddingAdapter.CohereBatchClient batchClient = texts -> {
            observedBatchSizes.add(texts.size());
            return texts.stream().map(text -> new float[] {text.length()}).toList();
        };

        CohereEmbeddingAdapter adapter = new CohereEmbeddingAdapter(batchClient, 200, 5, millis -> {
        });

        List<String> input = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            input.add("offer-" + i);
        }

        List<float[]> vectors = adapter.embed(input);

        assertEquals(200, vectors.size());
        assertEquals(List.of(96, 96, 8), observedBatchSizes);
    }

    @Test
    void shouldThrottleRequestsBetweenBatches() {
        AtomicInteger sleepCalls = new AtomicInteger();
        List<Long> sleepDurations = new ArrayList<>();

        CohereEmbeddingAdapter.Sleeper sleeper = millis -> {
            sleepCalls.incrementAndGet();
            sleepDurations.add(millis);
        };

        CohereEmbeddingAdapter.CohereBatchClient batchClient = texts -> texts.stream()
                .map(text -> new float[] {1.0f})
                .toList();

        CohereEmbeddingAdapter adapter = new CohereEmbeddingAdapter(batchClient, 1, 120, sleeper);

        adapter.embed(List.of("a", "b", "c"));

        assertEquals(2, sleepCalls.get());
        assertEquals(2, sleepDurations.size());
    }
}