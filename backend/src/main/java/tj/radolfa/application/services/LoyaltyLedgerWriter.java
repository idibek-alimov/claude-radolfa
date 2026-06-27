package tj.radolfa.application.services;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadLoyaltyLedgerPort;
import tj.radolfa.application.ports.out.SaveLoyaltyLedgerPort;
import tj.radolfa.domain.model.LotDraw;
import tj.radolfa.domain.model.LoyaltyLedgerEntry;
import tj.radolfa.domain.model.LoyaltyReason;
import tj.radolfa.domain.service.LoyaltyCalculator;

import java.time.Instant;
import java.util.List;

/**
 * Application-layer helper that centralises loyalty ledger row writes.
 *
 * <p>Not {@code @Transactional}: this component runs inside the caller's existing
 * transaction boundary (per Hexagonal guardrail — {@code @Transactional} belongs on
 * the service, not on helpers or adapters).
 *
 * <h3>Credit rows</h3>
 * Appends a single credit entry with {@code remaining_points = delta} and an
 * {@code expires_at} computed from the configurable TTL.
 *
 * <h3>Debit rows (FIFO)</h3>
 * Loads live lots (oldest first), runs the FIFO planner, then appends one debit
 * row per touched lot and updates that lot's {@code remaining_points}.
 *
 * <p>Both operations return the new cached balance so callers can update
 * {@code users.loyalty_points} in the same transaction without reloading the user.
 */
@Component
public class LoyaltyLedgerWriter {

    private final SaveLoyaltyLedgerPort saveLedgerPort;
    private final LoadLoyaltyLedgerPort loadLedgerPort;
    private final LoyaltyCalculator     calculator;

    public LoyaltyLedgerWriter(SaveLoyaltyLedgerPort saveLedgerPort,
                                LoadLoyaltyLedgerPort loadLedgerPort,
                                LoyaltyCalculator calculator) {
        this.saveLedgerPort = saveLedgerPort;
        this.loadLedgerPort = loadLedgerPort;
        this.calculator     = calculator;
    }

    /**
     * Appends one credit ledger row and returns the new balance.
     *
     * @param userId         user receiving the credit
     * @param amount         points credited (must be &gt; 0)
     * @param reason         credit reason ({@code EARN_CASHBACK}, {@code REVIEW_BONUS},
     *                       {@code RESTORE}, {@code MANUAL_ADJUSTMENT}, {@code OPENING_BALANCE})
     * @param orderId        order that triggered the credit; {@code null} for non-order events
     * @param actorUserId    ADMIN who issued a manual adjustment; {@code null} for system events
     * @param currentBalance the user's balance before this credit (= {@code users.loyalty_points})
     * @param ttlMonths      calendar-month lifetime for this lot (e.g. 12)
     * @return new balance ({@code currentBalance + amount})
     */
    public int credit(Long userId, int amount, LoyaltyReason reason,
                      Long orderId, Long actorUserId, int currentBalance, int ttlMonths) {
        int newBalance  = currentBalance + amount;
        Instant now       = Instant.now();
        Instant expiresAt = calculator.expiresAt(now, ttlMonths);
        saveLedgerPort.append(new LoyaltyLedgerEntry(
                null, userId, amount, reason,
                orderId, actorUserId,
                null,        // sourceLotId — null for credit rows
                amount,      // remainingPoints = full delta on creation
                expiresAt,
                newBalance,
                null));      // createdAt — set by @PrePersist
        return newBalance;
    }

    /**
     * FIFO-consumes {@code amount} points, appending one debit row per lot touched,
     * and decrements {@code remaining_points} on each consumed lot.
     *
     * <p>Callers that floor at zero must clamp {@code amount} to the available balance
     * before calling (so the live lots can cover it).
     *
     * @param userId         user whose points are debited
     * @param amount         points to consume (must equal the actual amount to debit,
     *                       already clamped by the caller if applicable)
     * @param reason         debit reason ({@code REDEEM}, {@code REVOKE}, {@code EXPIRE},
     *                       or negative {@code MANUAL_ADJUSTMENT})
     * @param orderId        order that triggered the debit; {@code null} if not order-related
     * @param actorUserId    ADMIN who issued a manual adjustment; {@code null} otherwise
     * @param currentBalance the user's balance before this debit (= {@code users.loyalty_points})
     * @return result containing the consumed amount and the new cached balance
     */
    public DebitResult debit(Long userId, int amount, LoyaltyReason reason,
                              Long orderId, Long actorUserId, int currentBalance) {
        List<LoyaltyLedgerEntry> liveLots = loadLedgerPort.findLiveLots(userId);
        List<LotDraw> draws = calculator.planConsumption(liveLots, amount);

        int runningBalance = currentBalance;
        for (LotDraw draw : draws) {
            runningBalance -= draw.drawAmount();
            saveLedgerPort.append(new LoyaltyLedgerEntry(
                    null, userId, -draw.drawAmount(), reason,
                    orderId, actorUserId,
                    draw.lotId(),  // sourceLotId — links to the credit row consumed
                    null,          // remainingPoints — null for debit rows
                    null,          // expiresAt — null for debit rows
                    runningBalance,
                    null));        // createdAt — set by @PrePersist
            saveLedgerPort.updateRemaining(draw.lotId(), draw.newRemaining());
        }
        return new DebitResult(amount, runningBalance);
    }

    /**
     * Expires one specific credit lot: appends a single {@code EXPIRE} debit row and
     * zeroes {@code remaining_points} on the lot.
     *
     * <p>Unlike {@link #debit} (which runs FIFO from the user's current balance), this
     * method targets an already-identified expired lot returned by
     * {@code LoadLoyaltyLedgerPort.findExpiredLots(now)}.
     *
     * @param lot            the expired credit lot (must have {@code remainingPoints > 0})
     * @param currentBalance the user's balance before this expiry
     * @return new balance ({@code currentBalance - lot.remainingPoints()})
     */
    public int expireLot(LoyaltyLedgerEntry lot, int currentBalance) {
        int amount     = lot.remainingPoints();
        int newBalance = currentBalance - amount;
        saveLedgerPort.append(new LoyaltyLedgerEntry(
                null, lot.userId(), -amount, LoyaltyReason.EXPIRE,
                null, null,
                lot.id(),  // sourceLotId — links back to the credit row being expired
                null, null,
                newBalance,
                null));    // createdAt set by @PrePersist
        saveLedgerPort.updateRemaining(lot.id(), 0);
        return newBalance;
    }

    /**
     * Result of a FIFO debit operation.
     *
     * @param consumed   the total points consumed (= the {@code amount} argument)
     * @param newBalance the user's cached balance after the debit
     */
    public record DebitResult(int consumed, int newBalance) {}
}
