package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Discount;

import java.util.List;
import java.util.Optional;

public interface LoadDiscountPort {

    Optional<Discount> findByExternalRuleId(String externalRuleId);

    /**
     * Returns all currently active discounts covering the given item code.
     * Active = not disabled AND current time within [validFrom, validUpto].
     */
    List<Discount> findActiveByItemCode(String itemCode);
}
