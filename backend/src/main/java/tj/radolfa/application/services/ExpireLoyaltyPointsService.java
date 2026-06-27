package tj.radolfa.application.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.out.LoadLoyaltyLedgerPort;
import tj.radolfa.domain.model.LoyaltyLedgerEntry;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Daily sweep that expires loyalty credit lots whose {@code expires_at} has passed.
 *
 * <p>Not {@code @Transactional}: delegates each user to {@link UserPointsExpiryService}
 * so that one failing user does not roll back the entire sweep. Mirrors
 * {@link MonthlyTierEvaluationService}.
 *
 * <p>Called by {@link tj.radolfa.infrastructure.scheduling.LoyaltyPointsExpiryJob}.
 */
@Slf4j
@Service
public class ExpireLoyaltyPointsService {

    private final LoadLoyaltyLedgerPort  loadLedgerPort;
    private final UserPointsExpiryService userPointsExpiryService;

    public ExpireLoyaltyPointsService(LoadLoyaltyLedgerPort loadLedgerPort,
                                      UserPointsExpiryService userPointsExpiryService) {
        this.loadLedgerPort          = loadLedgerPort;
        this.userPointsExpiryService = userPointsExpiryService;
    }

    /**
     * Finds all live lots past their expiry date, groups them by user, and expires each
     * user's lots in its own transaction.
     */
    public void expireDueLots() {
        List<LoyaltyLedgerEntry> expired = loadLedgerPort.findExpiredLots(Instant.now());
        if (expired.isEmpty()) {
            log.info("LoyaltyPointsExpiry: no expired lots found");
            return;
        }

        Map<Long, List<LoyaltyLedgerEntry>> byUser = expired.stream()
                .collect(Collectors.groupingBy(LoyaltyLedgerEntry::userId));

        int usersProcessed = 0;
        int lotsExpired    = 0;
        int pointsRemoved  = 0;
        int failures       = 0;

        for (Map.Entry<Long, List<LoyaltyLedgerEntry>> entry : byUser.entrySet()) {
            Long userId = entry.getKey();
            List<LoyaltyLedgerEntry> lots = entry.getValue();
            try {
                userPointsExpiryService.expire(userId, lots);
                usersProcessed++;
                lotsExpired   += lots.size();
                pointsRemoved += lots.stream().mapToInt(LoyaltyLedgerEntry::remainingPoints).sum();
            } catch (Exception e) {
                failures++;
                log.error("LoyaltyPointsExpiry: failed for userId={}: {}", userId, e.getMessage(), e);
            }
        }

        log.info("LoyaltyPointsExpiry complete — users={}, lots={}, points={}, failures={}",
                usersProcessed, lotsExpired, pointsRemoved, failures);
    }
}
