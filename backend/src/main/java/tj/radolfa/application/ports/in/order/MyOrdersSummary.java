package tj.radolfa.application.ports.in.order;

/**
 * Order-count summary for the customer "my orders" view, grouped by the same
 * whitelist as {@link MyOrderFilter}. {@code all} includes every status
 * (incl. {@code CANCELLED}, which has no dedicated pill).
 */
public record MyOrdersSummary(long all, long progress, long delivered, long returns) {
}
