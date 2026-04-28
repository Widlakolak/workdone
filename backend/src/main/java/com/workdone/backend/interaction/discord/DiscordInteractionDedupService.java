package com.workdone.backend.interaction.discord;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DiscordInteractionDedupService {

    private static final Duration TTL = Duration.ofMinutes(20);
    private final ConcurrentHashMap<String, Instant> processedInteractions = new ConcurrentHashMap<>();

    public boolean markIfNew(String interactionId) {
        if (interactionId == null || interactionId.isBlank()) {
            return true;
        }

        cleanup();
        Instant now = Instant.now();
        Instant previous = processedInteractions.putIfAbsent(interactionId, now);
        if (previous == null) {
            return true;
        }
        return previous.isBefore(now.minus(TTL)) && processedInteractions.replace(interactionId, previous, now);
    }

    private void cleanup() {
        Instant threshold = Instant.now().minus(TTL);
        processedInteractions.entrySet().removeIf(entry -> entry.getValue().isBefore(threshold));
    }
}