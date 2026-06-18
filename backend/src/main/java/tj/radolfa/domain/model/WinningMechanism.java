package tj.radolfa.domain.model;

/**
 * Which pricing mechanism produced a cart line's final unit price.
 *
 * <p>Exactly one mechanism ever applies per line — campaign discounts and the
 * loyalty tier discount are never stacked together (see {@code CartLinePricer}).
 */
public enum WinningMechanism {
    /** Neither a campaign discount nor a loyalty tier beat the original price. */
    NONE,
    /** A campaign discount produced the lower price. */
    CAMPAIGN,
    /** The user's loyalty tier discount produced the lower price. */
    LOYALTY
}
