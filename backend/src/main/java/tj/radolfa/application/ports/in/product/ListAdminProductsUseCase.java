package tj.radolfa.application.ports.in.product;

import tj.radolfa.application.readmodel.AdminProductRow;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.ProductStatus;

public interface ListAdminProductsUseCase {
    PageResult<AdminProductRow> execute(ProductStatus status, String search, int page, int size);
}
