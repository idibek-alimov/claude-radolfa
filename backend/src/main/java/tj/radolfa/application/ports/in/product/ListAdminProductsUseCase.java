package tj.radolfa.application.ports.in.product;

import tj.radolfa.application.readmodel.AdminProductRow;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.ProductStatus;

public interface ListAdminProductsUseCase {
    /**
     * @param sellerId nullable — {@code null} returns all products (MANAGER/ADMIN view);
     *                 a non-null value restricts to a single seller's products.
     */
    PageResult<AdminProductRow> execute(ProductStatus status, String search, int page, int size, Long sellerId);
}
