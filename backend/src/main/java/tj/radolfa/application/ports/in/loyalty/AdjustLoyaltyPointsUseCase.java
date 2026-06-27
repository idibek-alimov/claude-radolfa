package tj.radolfa.application.ports.in.loyalty;

/**
 * In-Port: ADMIN manual credit or debit of a user's loyalty points balance.
 *
 * <p>Each adjustment is written as a {@code MANUAL_ADJUSTMENT} ledger row, recording
 * the delta, the ADMIN's actor id, and a mandatory free-text note explaining the reason.
 * FIFO consumption applies for debits. Over-drawing the balance is rejected.
 */
public interface AdjustLoyaltyPointsUseCase {

    /**
     * @param command the adjustment command
     * @return the user's new loyalty points balance after the adjustment
     */
    int execute(Command command);

    /**
     * @param userId      the user whose balance is being adjusted
     * @param delta       signed points change: positive = credit, negative = debit; must not be zero
     * @param note        mandatory free-text reason supplied by the ADMIN (not blank)
     * @param actorUserId the ADMIN user performing the adjustment
     */
    record Command(Long userId, int delta, String note, Long actorUserId) {}
}
