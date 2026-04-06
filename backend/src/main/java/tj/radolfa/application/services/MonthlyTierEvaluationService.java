package tj.radolfa.application.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.out.LoadLoyaltyTierPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.domain.model.LoyaltyTier;
import tj.radolfa.domain.model.User;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Evaluates loyalty tier promotions and demotions for all non-permanent users
 * at the end of each calendar month.
 *
 * <p>Called by {@link tj.radolfa.infrastructure.scheduling.MonthlyTierEvaluationJob}.
 */
@Slf4j
@Service
public class MonthlyTierEvaluationService {

    private final LoadUserPort           loadUserPort;
    private final LoadLoyaltyTierPort    loadLoyaltyTierPort;
    private final UserTierEvaluatorService userTierEvaluatorService;

    public MonthlyTierEvaluationService(LoadUserPort loadUserPort,
                                        LoadLoyaltyTierPort loadLoyaltyTierPort,
                                        UserTierEvaluatorService userTierEvaluatorService) {
        this.loadUserPort            = loadUserPort;
        this.loadLoyaltyTierPort     = loadLoyaltyTierPort;
        this.userTierEvaluatorService = userTierEvaluatorService;
    }

    /**
     * Evaluates the previous calendar month for all non-permanent users.
     * Each user is saved in its own transaction to avoid a single oversized transaction.
     */
    public void evaluatePreviousMonth() {
        YearMonth previousMonth = YearMonth.now(ZoneOffset.UTC).minusMonths(1);
        Instant from = previousMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to   = previousMonth.atEndOfMonth().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<LoyaltyTier> allTiers = loadLoyaltyTierPort.findAll();
        List<User> users = loadUserPort.findAllNonPermanent();

        log.info("Monthly tier evaluation started for {} (evaluating {} users)", previousMonth, users.size());

        int promoted = 0, demoted = 0, unchanged = 0;

        for (User user : users) {
            try {
                Result result = userTierEvaluatorService.evaluate(user, from, to, allTiers);
                switch (result) {
                    case PROMOTED  -> promoted++;
                    case DEMOTED   -> demoted++;
                    case UNCHANGED -> unchanged++;
                }
            } catch (Exception e) {
                log.error("Failed to evaluate user id={}: {}", user.id(), e.getMessage(), e);
            }
        }

        log.info("Monthly tier evaluation complete — promoted={}, demoted={}, unchanged={}",
                promoted, demoted, unchanged);
    }

    public enum Result { PROMOTED, DEMOTED, UNCHANGED }
}
