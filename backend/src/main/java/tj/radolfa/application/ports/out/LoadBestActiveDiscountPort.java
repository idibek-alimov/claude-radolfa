package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Discount;

import java.util.Optional;

/**
 * Resolves the single best active discount for a given SKU item code,
 * using type-rank priority (lowest rank wins).
 */
public interface LoadBestActiveDiscountPort {

    Optional<Discount> findBestActiveForItemCode(String itemCode);
}
