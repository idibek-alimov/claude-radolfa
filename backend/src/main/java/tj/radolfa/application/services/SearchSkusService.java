package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.warehouse.SearchSkusUseCase;
import tj.radolfa.application.ports.out.InventoryPlacementPort;
import tj.radolfa.application.ports.out.LoadWarehousePort;
import tj.radolfa.application.ports.out.SearchSkusPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.PlacementView;
import tj.radolfa.domain.model.SkuSearchRow;

import java.util.List;
import java.util.Map;

@Service
public class SearchSkusService implements SearchSkusUseCase {

    private final SearchSkusPort         searchSkusPort;
    private final InventoryPlacementPort placementPort;
    private final LoadWarehousePort      loadWarehousePort;

    public SearchSkusService(SearchSkusPort searchSkusPort,
                             InventoryPlacementPort placementPort,
                             LoadWarehousePort loadWarehousePort) {
        this.searchSkusPort   = searchSkusPort;
        this.placementPort    = placementPort;
        this.loadWarehousePort = loadWarehousePort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SkuSearchRow> execute(String query, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(100, size));
        if (query == null || query.isBlank()) {
            return new PageResult<>(List.of(), 0, safePage, safeSize, true);
        }

        PageResult<SkuSearchRow> raw = searchSkusPort.search(query.trim(), safePage, safeSize);

        List<Long> skuIds = raw.content().stream().map(SkuSearchRow::skuId).toList();
        if (skuIds.isEmpty()) {
            return raw;
        }

        Long wh = loadWarehousePort.findDefault().id();
        Map<Long, List<PlacementView>> views = placementPort.placementViewsForSkus(skuIds, wh);

        List<SkuSearchRow> enriched = raw.content().stream()
                .map(r -> new SkuSearchRow(
                        r.skuId(), r.skuCode(), r.barcode(), r.sizeLabel(),
                        r.stockQuantity(), r.productName(),
                        views.getOrDefault(r.skuId(), List.of())))
                .toList();

        return new PageResult<>(enriched, raw.totalElements(), raw.number(), raw.size(), raw.last());
    }
}
