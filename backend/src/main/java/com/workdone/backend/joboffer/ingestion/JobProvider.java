package com.workdone.backend.joboffer.ingestion;

import com.workdone.backend.common.model.JobOfferRecord;

import java.util.List;

/**
 * Wspólny interfejs dla każdego źródła ofert (np. API, RSS). 
 * Każdy dostawca musi mieć nazwę i umieć pobrać listę ofert dla danego kontekstu.
 */
public interface JobProvider {

    enum Scope {
        GLOBAL,
        CONTEXTUAL
    }

    String sourceName();

    List<JobOfferRecord> fetchOffers(SearchContext context);

    default Scope scope() {
        return Scope.CONTEXTUAL;
    }

    default String requestKey(SearchContext context) {
        String location = (context.location() == null || context.isGlobalRemote())
                ? SearchContext.REMOTE_GLOBAL
                : context.location().trim().toLowerCase();
        String keywords = context.keywords() == null ? "" : String.join(",", context.keywords());
        return sourceName() + "|" + context.remoteOnly() + "|" + location + "|" + keywords;
    }
}