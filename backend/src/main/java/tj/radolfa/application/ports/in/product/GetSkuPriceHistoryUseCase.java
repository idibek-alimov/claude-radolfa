package tj.radolfa.application.ports.in.product;

import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.SkuPriceChange;

public interface GetSkuPriceHistoryUseCase {
    PageResult<SkuPriceChange> execute(Long skuId, String sortBy, String sortDir, int page, int size);
}
