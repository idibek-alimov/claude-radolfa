package tj.radolfa.application.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.discount.ListDiscountedProductsUseCase;
import tj.radolfa.application.ports.out.DiscountedProductFilter;
import tj.radolfa.application.ports.out.ListDiscountedProductsPort;
import tj.radolfa.domain.model.DiscountedProductRow;

@Service
@Transactional(readOnly = true)
public class ListDiscountedProductsService implements ListDiscountedProductsUseCase {

    private final ListDiscountedProductsPort port;

    public ListDiscountedProductsService(ListDiscountedProductsPort port) {
        this.port = port;
    }

    @Override
    public Page<DiscountedProductRow> execute(DiscountedProductFilter filter, Pageable pageable) {
        return port.findDiscountedProducts(filter, pageable);
    }
}
