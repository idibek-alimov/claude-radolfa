package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.LoadHomeCollectionsPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.infrastructure.persistence.adapter.DiscountEnrichmentAdapter.DiscountInfo;
import tj.radolfa.infrastructure.persistence.repository.ListingVariantRepository;
import tj.radolfa.infrastructure.persistence.repository.SkuRepository;
import tj.radolfa.application.readmodel.ListingVariantDto;
import tj.radolfa.application.readmodel.SkuDto;

import java.util.List;
import java.util.Map;

/**
 * Hexagonal adapter: SQL-backed queries for homepage collection sections.
 *
 * <p>Reuses the same {@code Object[]} column layout as the grid queries
 * in {@link ListingReadAdapter}, with batch-loaded images and SKUs.
 * Discounts are resolved from the discounts table post-query.
 */
@Component
public class HomeCollectionsAdapter implements LoadHomeCollectionsPort {

    private final ListingVariantRepository variantRepo;
    private final SkuRepository skuRepo;
    private final DiscountEnrichmentAdapter discountEnrichment;

    public HomeCollectionsAdapter(ListingVariantRepository variantRepo,
                                  SkuRepository skuRepo,
                                  DiscountEnrichmentAdapter discountEnrichment) {
        this.variantRepo = variantRepo;
        this.skuRepo = skuRepo;
        this.discountEnrichment = discountEnrichment;
    }

    // ---- Homepage preview (limited, no pagination metadata) ----

    @Override
    public List<ListingVariantDto> loadFeatured(int limit) {
        return toGridDtos(variantRepo.findFeaturedGrid(PageRequest.of(0, limit)).getContent());
    }

    @Override
    public List<ListingVariantDto> loadNewArrivals(int limit) {
        return toGridDtos(variantRepo.findNewArrivalsGrid(PageRequest.of(0, limit)).getContent());
    }

    @Override
    public List<ListingVariantDto> loadOnSale(int limit) {
        List<Long> variantIds = discountEnrichment.findVariantIdsWithActiveDiscounts();
        if (variantIds.isEmpty()) return List.of();

        List<Object[]> rows = variantRepo.findGridByVariantIds(variantIds, PageRequest.of(0, limit)).getContent();
        return toGridDtos(rows);
    }

    // ---- Paginated "View All" pages ----

    @Override
    public PageResult<ListingVariantDto> loadFeaturedPage(int page, int limit) {
        return toPageResult(variantRepo.findFeaturedGrid(PageRequest.of(page - 1, limit)), page, limit);
    }

    @Override
    public PageResult<ListingVariantDto> loadNewArrivalsPage(int page, int limit) {
        return toPageResult(variantRepo.findNewArrivalsGrid(PageRequest.of(page - 1, limit)), page, limit);
    }

    @Override
    public PageResult<ListingVariantDto> loadOnSalePage(int page, int limit) {
        List<Long> variantIds = discountEnrichment.findVariantIdsWithActiveDiscounts();
        if (variantIds.isEmpty()) {
            return new PageResult<>(List.of(), 0, page, limit, true);
        }

        Page<Object[]> raw = variantRepo.findGridByVariantIds(variantIds, PageRequest.of(page - 1, limit));
        return toPageResult(raw, page, limit);
    }

    // ---- Shared helpers (same column layout as ListingReadAdapter) ----

    private PageResult<ListingVariantDto> toPageResult(Page<Object[]> raw, int page, int limit) {
        List<ListingVariantDto> content = toGridDtos(raw.getContent());
        return new PageResult<>(content, raw.getTotalElements(), page, limit,
                (long) page * limit < raw.getTotalElements());
    }

    private List<ListingVariantDto> toGridDtos(List<Object[]> rows) {
        List<Long> variantIds = rows.stream()
                .map(row -> (Long) row[0])
                .toList();

        Map<Long, List<String>> imageMap = ListingGridRowMapper.loadImageMap(variantIds, variantRepo);
        Map<Long, DiscountInfo> discountMap = discountEnrichment.resolveForVariants(variantIds);
        Map<Long, List<SkuDto>> skuMap = ListingGridRowMapper.loadSkuMap(variantIds, skuRepo);

        return rows.stream()
                .map(row -> ListingGridRowMapper.toGridDto(row, imageMap, discountMap, skuMap))
                .toList();
    }
}
