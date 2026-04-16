package com.workdone.backend.interaction.discord;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/discord/interactions")
public class DiscordInteractionController {

    private static final int PING = 1;
    private static final int MESSAGE_COMPONENT = 3;

    private final DiscordInteractionService interactionService;

    public DiscordInteractionController(DiscordInteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> handleInteraction(
            @RequestHeader(value = "X-Signature-Ed25519", required = false) String signature,
            @RequestHeader(value = "X-Signature-Timestamp", required = false) String timestamp,
            @RequestBody DiscordInteractionRequest request) {

        if (request.type() == PING) {
            return ResponseEntity.ok(Map.of("type", 1));
        }

        if (request.type() == MESSAGE_COMPONENT && request.data() != null) {
            String resultMessage = interactionService.handleCustomId(request.data().customId());
            return ResponseEntity.ok(Map.of(
                    "type", 4,
                    "data", Map.of("content", resultMessage, "flags", 64)
            ));
        }

        return ResponseEntity.ok(Map.of(
                "type", 4,
                "data", Map.of("content", "Interakcja nieobsługiwana.", "flags", 64)
        ));
    }

    public record DiscordInteractionRequest(int type, DiscordInteractionData data) {
    }

    public record DiscordInteractionData(@JsonProperty("custom_id") String customId) {
    }
}