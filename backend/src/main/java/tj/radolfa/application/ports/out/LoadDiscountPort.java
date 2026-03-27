package tj.radolfa.application.ports.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.domain.model.Discount;

import java.util.List;
import java.util.Optional;

public interface LoadDiscountPort {

    Optional<Discount> findById(Long id);

    /**
     * Returns all currently active discounts covering the given item code.
     * Active = not disabled AND current time within [validFrom, validUpto].
     */
    List<Discount> findActiveByItemCode(String itemCode);

    Page<Discount> findAll(DiscountFilter filter, Pageable pageable);
}
