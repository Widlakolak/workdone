package com.workdone.backend.interaction.discord;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.crypto.tink.subtle.Ed25519Verify;
import com.google.crypto.tink.subtle.Hex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/discord/interactions")
public class DiscordInteractionController {

    private static final int PING = 1;
    private static final int MESSAGE_COMPONENT = 3;

    private final DiscordInteractionService interactionService;
    private final DiscordInteractionDedupService dedupService;
    private final byte[] decodedPublicKey;
    private final ObjectMapper objectMapper;

    public DiscordInteractionController(DiscordInteractionService interactionService,
                                        DiscordInteractionDedupService dedupService,
                                        @Value("${workdone.discord.public-key:}") String publicKey,
                                        ObjectMapper objectMapper) {
        this.interactionService = interactionService;
        this.dedupService = dedupService;
        this.objectMapper = objectMapper;
        this.decodedPublicKey = (publicKey != null && !publicKey.isBlank()) ? Hex.decode(publicKey) : null;
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
                log.info("Received PING from Discord, responding with PONG.");
                return ResponseEntity.ok(Map.of("type", 1));
            }

            if (request.type() == MESSAGE_COMPONENT && request.data() != null) {
                if (!dedupService.markIfNew(request.id())) {
                    log.info("♻️ Zduplikowana interakcja Discord pominięta: {}", request.id());
                    return ResponseEntity.ok(Map.of(
                            "type", 4,
                            "data", Map.of("content", "✅ Akcja już została przetworzona.", "flags", 64)
                    ));
                }
                String resultMessage = interactionService.handleCustomId(request.data().customId());
                return ResponseEntity.ok(Map.of(
                        "type", 4,
                        "data", Map.of("content", resultMessage, "flags", 64)
                ));
            }

        } catch (Exception e) {
            log.error("Error parsing Discord interaction: {}", e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
                "type", 4,
                "data", Map.of("content", "Interakcja nieobsługiwana.", "flags", 64)
        ));
    }

    private boolean verifySignature(String signature, String timestamp, String body) {
        if (decodedPublicKey == null) {
            log.warn("Discord Public Key is not configured! Skipping signature verification (UNSAFE).");
            return true;
        }

        if (signature == null || timestamp == null) {
            log.warn("Missing signature or timestamp header.");
            return false;
        }

        try {
            Ed25519Verify verifier = new Ed25519Verify(decodedPublicKey);
            byte[] message = (timestamp + body).getBytes(StandardCharsets.UTF_8);
            verifier.verify(Hex.decode(signature), message);
            return true;
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            log.warn("Invalid Discord signature: {}", e.getMessage());
            return false;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DiscordInteractionRequest(String id, int type, DiscordInteractionData data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DiscordInteractionData(@JsonProperty("custom_id") String customId) {}
}