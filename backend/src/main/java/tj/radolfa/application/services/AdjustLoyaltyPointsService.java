package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.loyalty.AdjustLoyaltyPointsUseCase;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.LoyaltyReason;
import tj.radolfa.domain.model.User;
import tj.radolfa.infrastructure.config.LoyaltyRewardProperties;

/**
 * ADMIN manual credit or debit of a user's loyalty points balance.
 *
 * <p>Each call writes a single {@code MANUAL_ADJUSTMENT} ledger row (for credits) or
 * one debit row per FIFO lot consumed (for debits), recording the actor's id and the
 * mandatory note. The cached {@code users.loyalty_points} is updated in the same
 * transaction.
 *
 * <p>Over-draw is rejected with {@link IllegalArgumentException} (→ 400).
 * {@code delta == 0} is also rejected.
 */
@Service
@Transactional
public class AdjustLoyaltyPointsService implements AdjustLoyaltyPointsUseCase {

    private final LoadUserPort            loadUserPort;
    private final SaveUserPort            saveUserPort;
    private final LoyaltyLedgerWriter     ledgerWriter;
    private final LoyaltyRewardProperties properties;

    public AdjustLoyaltyPointsService(LoadUserPort loadUserPort,
                                       SaveUserPort saveUserPort,
                                       LoyaltyLedgerWriter ledgerWriter,
                                       LoyaltyRewardProperties properties) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.ledgerWriter = ledgerWriter;
        this.properties   = properties;
    }

    @Override
    public int execute(Command command) {
        if (command.delta() == 0) {
            throw new IllegalArgumentException("Adjustment delta must not be zero.");
        }

        User user = loadUserPort.loadById(command.userId())
                .orElseThrow(() -> new IllegalStateException("User not found: " + command.userId()));

        LoyaltyProfile profile   = user.loyalty();
        int            oldPoints = profile.points();
        int            newBalance;

        if (command.delta() > 0) {
            newBalance = ledgerWriter.credit(
                    command.userId(), command.delta(),
                    LoyaltyReason.MANUAL_ADJUSTMENT,
                    null, command.actorUserId(),
                    oldPoints, properties.pointsTtlMonths(),
                    command.note());
        } else {
            int toDebit = -command.delta();  // positive amount to consume
            if (toDebit > oldPoints) {
                throw new IllegalArgumentException(
                        "Adjustment would overdraw the balance: requested debit " + toDebit
                        + " but available balance is " + oldPoints + ".");
            }
            LoyaltyLedgerWriter.DebitResult result = ledgerWriter.debit(
                    command.userId(), toDebit,
                    LoyaltyReason.MANUAL_ADJUSTMENT,
                    null, command.actorUserId(),
                    oldPoints, command.note());
            newBalance = result.newBalance();
        }

        LoyaltyProfile adjusted = new LoyaltyProfile(
                profile.tier(),
                newBalance,
                profile.spendToNextTier(),
                profile.spendToMaintainTier(),
                profile.currentMonthSpending(),
                profile.permanent(),
                profile.lowestTierEver());

        saveUserPort.save(new User(
                user.id(), user.phone(), user.role(), user.name(),
                user.email(), adjusted, user.enabled(), user.version()));

        return newBalance;
    }
}
