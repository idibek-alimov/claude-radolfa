package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.product.CountPendingProductsUseCase;
import tj.radolfa.application.ports.out.LoadAdminProductPagePort;
import tj.radolfa.domain.model.ProductStatus;

@Service
public class CountPendingProductsService implements CountPendingProductsUseCase {

    private final LoadAdminProductPagePort loadAdminProductPagePort;

    public CountPendingProductsService(LoadAdminProductPagePort loadAdminProductPagePort) {
        this.loadAdminProductPagePort = loadAdminProductPagePort;
    }

    @Override
    @Transactional(readOnly = true)
    public long execute() {
        return loadAdminProductPagePort.countByStatus(ProductStatus.PENDING_REVIEW);
    }
}
