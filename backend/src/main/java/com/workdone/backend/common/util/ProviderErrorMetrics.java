package com.workdone.backend.common.util;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@Component
public class ProviderErrorMetrics {

    private final ConcurrentHashMap<String, EnumMap<ProviderErrorType, LongAdder>> counters = new ConcurrentHashMap<>();

    public void increment(String providerName, ProviderErrorType errorType) {
        EnumMap<ProviderErrorType, LongAdder> providerCounters = counters.computeIfAbsent(providerName, key -> {
            EnumMap<ProviderErrorType, LongAdder> map = new EnumMap<>(ProviderErrorType.class);
            for (ProviderErrorType type : ProviderErrorType.values()) {
                map.put(type, new LongAdder());
            }
            return map;
        });
        providerCounters.get(errorType).increment();
    }

    public Map<String, Map<ProviderErrorType, Long>> snapshot() {
        Map<String, Map<ProviderErrorType, Long>> snapshot = new ConcurrentHashMap<>();
        counters.forEach((provider, values) -> {
            EnumMap<ProviderErrorType, Long> row = new EnumMap<>(ProviderErrorType.class);
            values.forEach((type, counter) -> row.put(type, counter.sum()));
            snapshot.put(provider, row);
        });
        return snapshot;
    }
}