package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Discount;

import java.util.Optional;

public interface LockDiscountForUsagePort {

    /** Acquires a pessimistic write lock on the discount row for the duration of the current transaction. */
    Optional<Discount> lockById(Long discountId);
}
