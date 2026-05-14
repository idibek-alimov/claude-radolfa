package tj.radolfa.application.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.out.LoadMonthlySpendingPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.LoyaltyTier;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.service.LoyaltyCalculator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Evaluates and persists the loyalty tier change for a single user.
 * Extracted into its own bean so that {@code @Transactional} is applied
 * through the Spring proxy (self-invocation from
 * {@link MonthlyTierEvaluationService} would bypass the proxy).
 */
@Slf4j
@Service
public class UserTierEvaluatorService {

    private final SaveUserPort            saveUserPort;
    private final LoadMonthlySpendingPort loadMonthlySpendingPort;
    private final LoyaltyCalculator       loyaltyCalculator;

    public UserTierEvaluatorService(SaveUserPort saveUserPort,
                                    LoadMonthlySpendingPort loadMonthlySpendingPort,
                                    LoyaltyCalculator loyaltyCalculator) {
        this.saveUserPort            = saveUserPort;
        this.loadMonthlySpendingPort = loadMonthlySpendingPort;
        this.loyaltyCalculator       = loyaltyCalculator;
    }

    @Transactional
    public MonthlyTierEvaluationService.Result evaluate(User user,
                                                        Instant from,
                                                        Instant to,
                                                        List<LoyaltyTier> allTiers) {
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
                return MonthlyTierEvaluationService.Result.PROMOTED;
            } else {
                log.debug("User {} demoted: {} → {}", user.id(), previousTierName, newTierName);
                return MonthlyTierEvaluationService.Result.DEMOTED;
            }
        }
        return MonthlyTierEvaluationService.Result.UNCHANGED;
    }

    private int tierRank(LoyaltyTier tier) {
        return tier != null ? tier.displayOrder() : -1;
    }
}
