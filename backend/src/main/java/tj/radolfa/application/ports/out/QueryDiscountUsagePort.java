package tj.radolfa.application.ports.out;

import java.util.Collection;
import java.util.Map;

public interface QueryDiscountUsagePort {

    /** Total usage counts across all users, keyed by discountId. */
    Map<Long, Long> countByDiscountIds(Collection<Long> discountIds);

    /** Per-user usage counts, keyed by discountId. */
    Map<Long, Long> countByDiscountIdsForUser(Collection<Long> discountIds, Long userId);
}
