package com.workdone.backend.interaction.discord;

import com.workdone.backend.model.JobOfferRecord;
import com.workdone.backend.model.OfferStatus;
import com.workdone.backend.storage.InMemoryOfferStore;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiscordInteractionServiceTest {

    @Test
    void shouldUpdateOfferStatusWhenCustomIdIsValid() {
        InMemoryOfferStore store = new InMemoryOfferStore();
        store.upsert(new JobOfferRecord(
                "id-1",
                "fp-1",
                "Java Developer",
                "ACME",
                "https://example.com/job/1",
                "Łódź",
                "desc",
                "",
                List.of("Java"),
                95.0,
                OfferStatus.ANALYZED,
                LocalDateTime.now(),
                "JUST_JOIN_IT"
        ));

        DiscordInteractionService service = new DiscordInteractionService(store);
        String result = service.handleCustomId("applied|https://example.com/job/1");

        assertThat(result).contains("APPLIED");
        JobOfferRecord updated = store.findByUrls(List.of("https://example.com/job/1")).getFirst();
        assertThat(updated.status()).isEqualTo(OfferStatus.APPLIED);
    }
}