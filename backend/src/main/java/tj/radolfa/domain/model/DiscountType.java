package tj.radolfa.domain.model;

import java.util.Objects;

/**
 * A named discount type with an explicit priority rank.
 * Lower rank = higher priority (FLASH_SALE=1 beats SEASONAL=3).
 * Managed by ADMIN via REST; stored in the {@code discount_types} table.
 */
public record DiscountType(Long id, String name, int rank, StackingPolicy stackingPolicy) {

    public DiscountType {
        Objects.requireNonNull(name, "name must not be null");
        if (stackingPolicy == null) stackingPolicy = StackingPolicy.BEST_WINS;
    }
}
