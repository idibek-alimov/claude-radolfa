package tj.radolfa.application.ports.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.domain.model.DiscountChange;

public interface LoadDiscountChangePort {

    Page<DiscountChange> findByDiscountId(Long discountId, Pageable pageable);
}
