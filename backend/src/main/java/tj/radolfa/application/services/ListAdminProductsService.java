package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.product.ListAdminProductsUseCase;
import tj.radolfa.application.ports.out.LoadAdminProductPagePort;
import tj.radolfa.application.readmodel.AdminProductRow;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.ProductStatus;

@Service
public class ListAdminProductsService implements ListAdminProductsUseCase {

    private final LoadAdminProductPagePort loadAdminProductPagePort;

    public ListAdminProductsService(LoadAdminProductPagePort loadAdminProductPagePort) {
        this.loadAdminProductPagePort = loadAdminProductPagePort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminProductRow> execute(ProductStatus status, String search, int page, int size, Long sellerId) {
        return loadAdminProductPagePort.findAdminPage(status, search, page, size, sellerId);
    }
}
