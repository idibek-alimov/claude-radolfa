package tj.radolfa.application.ports.in.discount;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.application.ports.out.DiscountedProductFilter;
import tj.radolfa.domain.model.DiscountedProductRow;

public interface ListDiscountedProductsUseCase {
    Page<DiscountedProductRow> execute(DiscountedProductFilter filter, Pageable pageable);
}
