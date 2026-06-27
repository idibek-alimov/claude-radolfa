package tj.radolfa.domain.model;

/**
 * Describes the business reason for a {@link LoyaltyLedgerEntry} movement.
 *
 * <ul>
 *   <li><b>Credit reasons</b> (delta &gt; 0): {@code EARN_CASHBACK}, {@code REVIEW_BONUS},
 *       {@code RESTORE}, positive {@code MANUAL_ADJUSTMENT}, {@code OPENING_BALANCE}.</li>
 *   <li><b>Debit reasons</b> (delta &lt; 0): {@code REDEEM}, {@code REVOKE},
 *       {@code EXPIRE}, negative {@code MANUAL_ADJUSTMENT}.</li>
 * </ul>
 */
public enum LoyaltyReason {

    /** Cashback awarded after a successful payment (order → PAID). */
    EARN_CASHBACK,

    /** Flat bonus awarded when a product review is approved. */
    REVIEW_BONUS,

    /** Points redeemed as a discount at checkout. */
    REDEEM,

    /** Redeemed points credited back when an order is cancelled, expired, or recalled. */
    RESTORE,

    /** Cashback points revoked when a payment is refunded, or as saga compensation. */
    REVOKE,

    /** Points forfeited because a credit lot reached its expiry date. */
    EXPIRE,

    /** Manual credit or debit applied by an ADMIN, with a mandatory reason text. */
    MANUAL_ADJUSTMENT,

    /** Synthetic opening row created during the initial ledger migration for existing balances. */
    OPENING_BALANCE
}
