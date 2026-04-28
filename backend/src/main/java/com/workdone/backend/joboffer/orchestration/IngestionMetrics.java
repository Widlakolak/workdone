package com.workdone.backend.joboffer.orchestration;

import lombok.Data;

@Data
public class IngestionMetrics {
    int totalFetched = 0;
    int alreadyExists = 0;
    int failedMustHave = 0;
    int processedPreAI = 0;
    int aiCacheHits = 0;
    int aiCalls = 0;
    int aiSkips = 0;
    int instantFound = 0;

    @Override
    public String toString() {
        return String.format(
                "\n📊 --- LEJEK INGESTII ---" +
                        "\n📥 Pobrano: %d" +
                        "\n⏭️ Już istniejące: %d" +
                        "\n❌ Odrzucone (Must-Have): %d" +
                        "\n⚙️ Do procesowania: %d" +
                        "\n🧠 AI: %d wezwań, %d z cache, %d pominięto" +
                        "\n🏆 INSTANT: %d",
                totalFetched, alreadyExists, failedMustHave, processedPreAI, aiCalls, aiCacheHits, aiSkips, instantFound
        );
    }
}
