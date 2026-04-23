package com.workdone.backend.interaction.discord;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.crypto.tink.subtle.Ed25519Verify;
import com.google.crypto.tink.subtle.Hex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.GeneralSecurityException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/discord/interactions")
public class DiscordInteractionController {

    private static final int PING = 1;
    private static final int MESSAGE_COMPONENT = 3;

    private final DiscordInteractionService interactionService;
    private final String publicKey;
    private final ObjectMapper objectMapper;

    public DiscordInteractionController(DiscordInteractionService interactionService,
                                        @Value("${workdone.discord.public-key:}") String publicKey,
                                        ObjectMapper objectMapper) {
        this.interactionService = interactionService;
        this.publicKey = publicKey;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> handleInteraction(
            @RequestHeader(value = "X-Signature-Ed25519", required = false) String signature,
            @RequestHeader(value = "X-Signature-Timestamp", required = false) String timestamp,
            @RequestBody String rawBody) {

        if (!verifySignature(signature, timestamp, rawBody)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            DiscordInteractionRequest request = objectMapper.readValue(rawBody, DiscordInteractionRequest.class);

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

        } catch (Exception e) {
            log.error("Error parsing Discord interaction", e);
        }

        return ResponseEntity.ok(Map.of(
                "type", 4,
                "data", Map.of("content", "Interakcja nieobsługiwana.", "flags", 64)
        ));
    }

    private boolean verifySignature(String signature, String timestamp, String body) {
        if (publicKey == null || publicKey.isBlank()) {
            log.warn("Discord Public Key is not configured! Skipping signature verification (UNSAFE).");
            return true;
        }

        if (signature == null || timestamp == null) {
            log.warn("Missing signature or timestamp header.");
            return false;
        }

        try {
            Ed25519Verify verifier = new Ed25519Verify(Hex.decode(publicKey));
            verifier.verify(Hex.decode(signature), (timestamp + body).getBytes());
            return true;
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            log.warn("Invalid Discord signature: {}", e.getMessage());
            return false;
        }
    }

    public record DiscordInteractionRequest(int type, DiscordInteractionData data) {}
    public record DiscordInteractionData(@JsonProperty("custom_id") String customId) {}
}
