package com.workdone.backend.interaction.discord;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiscordInteractionDedupServiceTest {

    private final DiscordInteractionDedupService service = new DiscordInteractionDedupService();

    @Test
    void shouldMarkFirstInteractionAsNew() {
        assertThat(service.markIfNew("abc-123")).isTrue();
    }

    @Test
    void shouldRejectDuplicateInteractionId() {
        assertThat(service.markIfNew("abc-123")).isTrue();
        assertThat(service.markIfNew("abc-123")).isFalse();
    }
}