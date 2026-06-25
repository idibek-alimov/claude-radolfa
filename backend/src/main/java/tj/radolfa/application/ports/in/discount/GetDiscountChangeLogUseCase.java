package tj.radolfa.application.ports.in.discount;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.domain.model.DiscountChange;

public interface GetDiscountChangeLogUseCase {

    Page<DiscountChange> execute(Long discountId, Pageable pageable);
}
