package tj.radolfa.infrastructure.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tj.radolfa.application.services.ExpireLoyaltyPointsService;

/**
 * Daily job that expires loyalty credit lots past their configured TTL.
 *
 * <p>Runs at 02:30 UTC every day (offset from {@code MonthlyTierEvaluationJob}'s
 * 02:00 UTC to avoid contention on the first of the month).
 */
@Slf4j
@Component
public class LoyaltyPointsExpiryJob {

    private final ExpireLoyaltyPointsService expiryService;

    public LoyaltyPointsExpiryJob(ExpireLoyaltyPointsService expiryService) {
        this.expiryService = expiryService;
    }

    @Scheduled(cron = "0 30 2 * * *", zone = "UTC")
    public void run() {
        log.info("LoyaltyPointsExpiryJob triggered");
        expiryService.expireDueLots();
    }
}
