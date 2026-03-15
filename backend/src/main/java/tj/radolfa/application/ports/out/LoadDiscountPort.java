package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Discount;

import java.util.List;
import java.util.Optional;

public interface LoadDiscountPort {

    Optional<Discount> findByErpPricingRuleId(String erpPricingRuleId);

    /**
     * Returns all currently active discounts for the given item code.
     * Active = not disabled AND current time within [validFrom, validUpto].
     * Filtering is done at the database level for performance.
     */
    List<Discount> findActiveByItemCode(String itemCode);
}
