package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.product.GetSkuPriceHistoryUseCase;
import tj.radolfa.application.ports.out.LoadSkuPriceHistoryPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.SkuPriceChange;

import java.util.Set;

@Service
@Transactional(readOnly = true)
public class GetSkuPriceHistoryService implements GetSkuPriceHistoryUseCase {

    private static final Set<String> SORTABLE_FIELDS = Set.of("occurredAt", "newPrice");

    private final LoadSkuPriceHistoryPort loadSkuPriceHistoryPort;

    public GetSkuPriceHistoryService(LoadSkuPriceHistoryPort loadSkuPriceHistoryPort) {
        this.loadSkuPriceHistoryPort = loadSkuPriceHistoryPort;
    }

    @Override
    public PageResult<SkuPriceChange> execute(Long skuId, String sortBy, String sortDir, int page, int size) {
        String safeSortBy  = SORTABLE_FIELDS.contains(sortBy) ? sortBy : "occurredAt";
        String safeSortDir = "ASC".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";
        int safePage        = Math.max(1, page);
        int safeSize        = Math.min(Math.max(1, size), 100);

        return loadSkuPriceHistoryPort.findBySkuId(skuId, safeSortBy, safeSortDir, safePage, safeSize);
    }
}
