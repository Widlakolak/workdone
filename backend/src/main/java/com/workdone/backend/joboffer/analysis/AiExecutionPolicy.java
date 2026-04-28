package com.workdone.backend.joboffer.analysis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class AiExecutionPolicy {
    private static final int MAX_AI_PER_RUN = 3;
    private static final double MIN_SEMANTIC_FOR_AI = 60.0;
    private final AtomicInteger currentRunUsage = new AtomicInteger(0);

    public void resetBudget() {
        log.info("📊 [POLICY] Resetowanie budżetu AI. W poprzednim runie zużyto: {}/{}", currentRunUsage.get(), MAX_AI_PER_RUN);
        currentRunUsage.set(0);
    }

    public boolean canExecute(double semanticScore) {
        if (currentRunUsage.get() >= MAX_AI_PER_RUN) {
            return false;
        }
        return semanticScore >= MIN_SEMANTIC_FOR_AI;
    }

    public void recordExecution() {
        currentRunUsage.incrementAndGet();
    }

    public int getRemainingBudget() {
        return Math.max(0, MAX_AI_PER_RUN - currentRunUsage.get());
    }
}