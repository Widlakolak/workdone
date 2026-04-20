package com.workdone.backend.format;

import com.workdone.backend.model.JobOfferRecord;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class OfferContentBuilder {

    public String buildTechnicalContent(JobOfferRecord offer) {
        return String.format("%s %s %s %s",
                offer.title(),
                offer.companyName(),
                offer.location(),
                offer.rawDescription()
        );
    }

    public String buildInstantMessage(JobOfferRecord offer) {
        return """
               🔥 **Instant Match >=90%%**
               %s @ %s
               Match: %s%%
               Priority: %s%%
               %s
               """.formatted(
                offer.title(),
                offer.companyName(),
                formatScore(offer.matchingScore()),
                formatScore(offer.priorityScore()),
                offer.sourceUrl()
        ).trim();
    }

    public String buildDigestMessage(List<JobOfferRecord> offers) {
        StringBuilder message = new StringBuilder("📋 **Daily Digest (>=60%)**\n");
        offers.stream()
                .sorted((a, b) -> Double.compare(safeValue(b.priorityScore()), safeValue(a.priorityScore())))
                .limit(20)
                .forEach(offer -> message.append(String.format(
                        " [M:%d%% | P:%d%%]\n- %s @ %s\n%s\n",
                        Math.round(safeValue(offer.matchingScore())),
                        Math.round(safeValue(offer.priorityScore())),
                        offer.title(),
                        offer.companyName(),
                        offer.sourceUrl()
                )));
        return message.toString();
    }

    private String formatScore(Double score) {
        return score == null ? "-" : String.valueOf(Math.round(score));
    }

    private double safeValue(Double value) {
        return value == null ? 0.0 : value;
    }
}