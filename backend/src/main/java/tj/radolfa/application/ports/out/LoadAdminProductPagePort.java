package tj.radolfa.application.ports.out;

import tj.radolfa.application.readmodel.AdminProductRow;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.ProductStatus;

public interface LoadAdminProductPagePort {
    /**
     * @param sellerId nullable — {@code null} disables the filter (all products).
     */
    PageResult<AdminProductRow> findAdminPage(ProductStatus status, String search, int page, int size, Long sellerId);
    long countByStatus(ProductStatus status);
}
