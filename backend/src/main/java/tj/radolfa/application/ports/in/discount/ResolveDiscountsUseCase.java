package tj.radolfa.application.ports.in.discount;

import tj.radolfa.domain.model.Discount;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ResolveDiscountsUseCase {

    /**
     * Resolves eligible discount campaigns for the given SKU item codes and returns them
     * in application order: BEST_WINS winner first, then STACKABLE campaigns.
     * Price folding (actual unit price computation) is the caller's responsibility via
     * {@link tj.radolfa.domain.model.AppliedDiscount#fold}.
     *
     * @return map of itemCode -> ordered list of discounts to apply;
     *         item codes with no applicable discount are omitted
     */
    Map<String, List<Discount>> resolve(Query q);

    record Query(
            List<String> itemCodes,
            Long userId,            // null for guests
            BigDecimal cartSubtotal, // null for listing-time (min-basket gate permissive)
            String couponCode        // null for listings; set at checkout when cart has a coupon
    ) {}
}
