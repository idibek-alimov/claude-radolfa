package tj.radolfa.application.ports.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.domain.model.DiscountedProductRow;

public interface ListDiscountedProductsPort {
    Page<DiscountedProductRow> findDiscountedProducts(DiscountedProductFilter filter, Pageable pageable);
}
