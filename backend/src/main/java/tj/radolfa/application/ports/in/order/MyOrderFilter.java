package tj.radolfa.application.ports.in.order;

import tj.radolfa.domain.model.OrderStatus;

import java.util.Set;

/**
 * Server-side status whitelist for the customer "my orders" filter pills.
 * {@link #ALL} carries an empty status set, meaning "no filter" (every status,
 * including {@code CANCELLED}, which has no dedicated pill).
 */
public enum MyOrderFilter {

    ALL(Set.of()),
    PROGRESS(Set.of(
            OrderStatus.PENDING,
            OrderStatus.PAID,
            OrderStatus.PICKED,
            OrderStatus.CLAIMED,
            OrderStatus.SHIPPED,
            OrderStatus.OUT_FOR_DELIVERY,
            OrderStatus.DELIVERY_ATTEMPTED,
            OrderStatus.READY_FOR_PICKUP,
            OrderStatus.RECALL_REQUESTED)),
    DELIVERED(Set.of(OrderStatus.DELIVERED)),
    RETURNS(Set.of(
            OrderStatus.RETURN_INITIATED,
            OrderStatus.RETURNED_TO_WAREHOUSE,
            OrderStatus.REFUNDED));

    private final Set<OrderStatus> statuses;

    MyOrderFilter(Set<OrderStatus> statuses) {
        this.statuses = statuses;
    }

    /** Empty for {@link #ALL} — callers should treat that as "no status filter". */
    public Set<OrderStatus> statuses() {
        return statuses;
    }

    /** Case-insensitive lookup; null or unrecognized values default to {@link #ALL}. */
    public static MyOrderFilter fromParam(String value) {
        if (value == null) {
            return ALL;
        }
        for (MyOrderFilter filter : values()) {
            if (filter.name().equalsIgnoreCase(value)) {
                return filter;
            }
        }
        return ALL;
    }
}
