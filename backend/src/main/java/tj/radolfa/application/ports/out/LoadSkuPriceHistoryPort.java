package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.SkuPriceChange;

public interface LoadSkuPriceHistoryPort {
    PageResult<SkuPriceChange> findBySkuId(Long skuId, String sortBy, String sortDir, int page, int size);
}
