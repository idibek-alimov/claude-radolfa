package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.loyalty.RedeemLoyaltyPointsUseCase;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.application.services.LoyaltyLedgerWriter.DebitResult;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.LoyaltyReason;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.service.LoyaltyCalculator;

/**
 * Deducts loyalty points from a user's balance during checkout.
 *
 * <p>Points are deducted <em>pessimistically</em> (before payment) to prevent
 * double-spend across concurrent sessions. If the payment is subsequently
 * refunded or cancelled, the calling service is responsible for restoring
 * the deducted points.
 *
 * <p>The deduction is written to the ledger as one {@code REDEEM} debit row
 * per credit lot consumed (FIFO, oldest lot first). The cached balance on
 * {@code users.loyalty_points} is updated in the same transaction.
 */
@Service
@Transactional
public class RedeemLoyaltyPointsService implements RedeemLoyaltyPointsUseCase {

    private final LoadUserPort        loadUserPort;
    private final SaveUserPort        saveUserPort;
    private final LoyaltyCalculator   loyaltyCalculator;
    private final LoyaltyLedgerWriter ledgerWriter;

    public RedeemLoyaltyPointsService(LoadUserPort loadUserPort,
                                      SaveUserPort saveUserPort,
                                      LoyaltyCalculator loyaltyCalculator,
                                      LoyaltyLedgerWriter ledgerWriter) {
        this.loadUserPort      = loadUserPort;
        this.saveUserPort      = saveUserPort;
        this.loyaltyCalculator = loyaltyCalculator;
        this.ledgerWriter      = ledgerWriter;
    }

    @Override
    public Money execute(Long userId, int pointsToRedeem) {
        if (pointsToRedeem <= 0) {
            throw new IllegalArgumentException("pointsToRedeem must be positive, got: " + pointsToRedeem);
        }

        User user = loadUserPort.loadById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));

        LoyaltyProfile profile = user.loyalty();
        if (profile.points() < pointsToRedeem) {
            throw new IllegalArgumentException(
                    "Insufficient points: available=" + profile.points() +
                    ", requested=" + pointsToRedeem);
        }

        Money moneyValue = loyaltyCalculator.pointsToMoney(pointsToRedeem);

        DebitResult result = ledgerWriter.debit(
                userId, pointsToRedeem, LoyaltyReason.REDEEM,
                null, null, profile.points());

        LoyaltyProfile deducted = new LoyaltyProfile(
                profile.tier(),
                result.newBalance(),
                profile.spendToNextTier(),
                profile.spendToMaintainTier(),
                profile.currentMonthSpending(),
                profile.permanent(),
                profile.lowestTierEver());

        saveUserPort.save(new User(
                user.id(), user.phone(), user.role(), user.name(),
                user.email(), deducted, user.enabled(), user.version()));

        return moneyValue;
    }
}
