package tj.radolfa.application.ports.out;

import tj.radolfa.application.readmodel.AdminProductRow;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.ProductStatus;

public interface LoadAdminProductPagePort {
    PageResult<AdminProductRow> findAdminPage(ProductStatus status, String search, int page, int size);
    long countByStatus(ProductStatus status);
}
