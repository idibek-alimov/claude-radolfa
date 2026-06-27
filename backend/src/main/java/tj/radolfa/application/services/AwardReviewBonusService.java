package tj.radolfa.application.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.loyalty.AwardReviewBonusUseCase;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.LoyaltyReason;
import tj.radolfa.domain.model.User;
import tj.radolfa.infrastructure.config.LoyaltyRewardProperties;

@Slf4j
@Service
@Transactional
public class AwardReviewBonusService implements AwardReviewBonusUseCase {

    private final LoadUserPort            loadUserPort;
    private final SaveUserPort            saveUserPort;
    private final LoyaltyRewardProperties properties;
    private final LoyaltyLedgerWriter     ledgerWriter;

    public AwardReviewBonusService(LoadUserPort loadUserPort,
                                   SaveUserPort saveUserPort,
                                   LoyaltyRewardProperties properties,
                                   LoyaltyLedgerWriter ledgerWriter) {
        this.loadUserPort  = loadUserPort;
        this.saveUserPort  = saveUserPort;
        this.properties    = properties;
        this.ledgerWriter  = ledgerWriter;
    }

    @Override
    public void execute(Long userId) {
        User user = loadUserPort.loadById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));

        LoyaltyProfile current = user.loyalty() != null ? user.loyalty() : LoyaltyProfile.empty();
        int bonus     = properties.reviewRewardPoints();
        int oldPoints = current.points();

        ledgerWriter.credit(userId, bonus, LoyaltyReason.REVIEW_BONUS,
                null, null, oldPoints, properties.pointsTtlMonths());

        LoyaltyProfile updated = new LoyaltyProfile(
                current.tier(),
                oldPoints + bonus,
                current.spendToNextTier(),
                current.spendToMaintainTier(),
                current.currentMonthSpending(),
                current.permanent(),
                current.lowestTierEver()
        );

        saveUserPort.save(new User(
                user.id(), user.phone(), user.role(), user.name(),
                user.email(), updated, user.enabled(), user.version()
        ));

        log.info("[LOYALTY] Awarded {} review-bonus points to userId={}", bonus, userId);
    }
}
