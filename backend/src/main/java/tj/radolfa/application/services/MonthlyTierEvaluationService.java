package tj.radolfa.application.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.out.LoadLoyaltyTierPort;
import tj.radolfa.application.ports.out.LoadMonthlySpendingPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.LoyaltyTier;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.service.LoyaltyCalculator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

/**
 * Evaluates loyalty tier promotions and demotions for all non-permanent users
 * at the end of each calendar month.
 *
 * <p>Called by {@link tj.radolfa.infrastructure.scheduling.MonthlyTierEvaluationJob}.
 */
@Slf4j
@Service
public class MonthlyTierEvaluationService {

    private final LoadUserPort            loadUserPort;
    private final SaveUserPort            saveUserPort;
    private final LoadLoyaltyTierPort     loadLoyaltyTierPort;
    private final LoadMonthlySpendingPort loadMonthlySpendingPort;
    private final LoyaltyCalculator       loyaltyCalculator;

    public MonthlyTierEvaluationService(LoadUserPort loadUserPort,
                                        SaveUserPort saveUserPort,
                                        LoadLoyaltyTierPort loadLoyaltyTierPort,
                                        LoadMonthlySpendingPort loadMonthlySpendingPort,
                                        LoyaltyCalculator loyaltyCalculator) {
        this.loadUserPort            = loadUserPort;
        this.saveUserPort            = saveUserPort;
        this.loadLoyaltyTierPort     = loadLoyaltyTierPort;
        this.loadMonthlySpendingPort = loadMonthlySpendingPort;
        this.loyaltyCalculator       = loyaltyCalculator;
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
                Result result = evaluateUser(user, from, to, allTiers);
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

    @Transactional
    public Result evaluateUser(User user, Instant from, Instant to, List<LoyaltyTier> allTiers) {
        BigDecimal netSpending = loadMonthlySpendingPort.calculateNetSpending(user.id(), from, to);
        LoyaltyProfile current = user.loyalty();
        LoyaltyProfile updated = loyaltyCalculator.evaluateMonthlyTier(netSpending, current, allTiers);

        saveUserPort.save(new User(
                user.id(), user.phone(), user.role(), user.name(),
                user.email(), updated, user.enabled(), user.version()));

        String previousTierName = current.tier() != null ? current.tier().name() : null;
        String newTierName      = updated.tier() != null ? updated.tier().name() : null;

        if (!Objects.equals(previousTierName, newTierName)) {
            if (tierRank(updated.tier()) > tierRank(current.tier())) {
                log.debug("User {} promoted: {} → {}", user.id(), previousTierName, newTierName);
                return Result.PROMOTED;
            } else {
                log.debug("User {} demoted: {} → {}", user.id(), previousTierName, newTierName);
                return Result.DEMOTED;
            }
        }
        return Result.UNCHANGED;
    }

    private int tierRank(LoyaltyTier tier) {
        return tier != null ? tier.displayOrder() : -1;
    }

    public enum Result { PROMOTED, DEMOTED, UNCHANGED }
}
