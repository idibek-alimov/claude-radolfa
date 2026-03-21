package tj.radolfa.infrastructure.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tj.radolfa.application.services.MonthlyTierEvaluationService;

/**
 * Scheduled job that triggers monthly loyalty tier evaluation.
 *
 * <p>Runs at 02:00 UTC on the 1st of each month. Evaluates the completed
 * previous calendar month's net spending for all non-permanent users and
 * promotes or demotes tiers accordingly.
 */
@Slf4j
@Component
public class MonthlyTierEvaluationJob {

    private final MonthlyTierEvaluationService evaluationService;

    public MonthlyTierEvaluationJob(MonthlyTierEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @Scheduled(cron = "0 0 2 1 * *", zone = "UTC")
    public void run() {
        log.info("MonthlyTierEvaluationJob triggered");
        evaluationService.evaluatePreviousMonth();
    }
}
