package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.LoadHomeCollectionsPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.infrastructure.persistence.adapter.DiscountEnrichmentAdapter.DiscountInfo;
import tj.radolfa.infrastructure.persistence.repository.ListingVariantRepository;
import tj.radolfa.infrastructure.web.dto.ListingVariantDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Hexagonal adapter: SQL-backed queries for homepage collection sections.
 *
 * <p>Reuses the same {@code Object[]} column layout as the grid queries
 * in {@link ListingReadAdapter}, with batch-loaded images.
 * Discounts are resolved from the discounts table post-query.
 */
@Component
public class HomeCollectionsAdapter implements LoadHomeCollectionsPort {

    private final ListingVariantRepository variantRepo;
    private final DiscountEnrichmentAdapter discountEnrichment;

    public HomeCollectionsAdapter(ListingVariantRepository variantRepo,
                                  DiscountEnrichmentAdapter discountEnrichment) {
        this.variantRepo = variantRepo;
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
        return toPageResult(variantRepo.findFeaturedGrid(PageRequest.of(page - 1, limit)), page);
    }

    @Override
    public PageResult<ListingVariantDto> loadNewArrivalsPage(int page, int limit) {
        return toPageResult(variantRepo.findNewArrivalsGrid(PageRequest.of(page - 1, limit)), page);
    }

    @Override
    public PageResult<ListingVariantDto> loadOnSalePage(int page, int limit) {
        List<Long> variantIds = discountEnrichment.findVariantIdsWithActiveDiscounts();
        if (variantIds.isEmpty()) {
            return new PageResult<>(List.of(), 0, page, false);
        }

        Page<Object[]> raw = variantRepo.findGridByVariantIds(variantIds, PageRequest.of(page - 1, limit));
        return toPageResult(raw, page);
    }

    // ---- Shared helpers (same column layout as ListingReadAdapter) ----

    private PageResult<ListingVariantDto> toPageResult(Page<Object[]> raw, int page) {
        List<ListingVariantDto> items = toGridDtos(raw.getContent());
        return new PageResult<>(items, raw.getTotalElements(), page,
                (long) page * raw.getSize() < raw.getTotalElements());
    }

    private List<ListingVariantDto> toGridDtos(List<Object[]> rows) {
        List<Long> variantIds = rows.stream()
                .map(row -> (Long) row[0])
                .toList();

        Map<Long, List<String>> imageMap = loadImageMap(variantIds);
        Map<Long, DiscountInfo> discountMap = discountEnrichment.resolveForVariants(variantIds);

        return rows.stream()
                .map(row -> toGridDto(row, imageMap, discountMap))
                .toList();
    }

    // Column layout (11 columns):
    // [0]=id, [1]=slug, [2]=name, [3]=categoryName, [4]=colorKey,
    // [5]=webDescription, [6]=topSelling,
    // [7]=MIN(originalPrice),
    // [8]=totalStock, [9]=colorHexCode, [10]=featured
    private ListingVariantDto toGridDto(Object[] row, Map<Long, List<String>> imageMap,
                                        Map<Long, DiscountInfo> discountMap) {
        Long id = (Long) row[0];
        DiscountInfo discount = discountMap.get(id);

        // When a discount exists, use the originalPrice from the same SKU
        // as the discountedPrice to keep strike-through pricing consistent
        BigDecimal originalPrice = discount != null
                ? discount.originalPrice()
                : toBigDecimal(row[7]);

        return new ListingVariantDto(
                id,
                (String) row[1],   // slug
                (String) row[2],   // name
                (String) row[3],   // category
                (String) row[4],   // colorKey
                (String) row[9],   // colorHexCode
                (String) row[5],   // webDescription
                imageMap.getOrDefault(id, List.of()),
                originalPrice,
                discount != null ? discount.discountedPrice() : null,
                null,                  // loyaltyPrice (enriched by controller)
                discount != null ? discount.discountPercentage() : null,
                null,                  // loyaltyDiscountPercentage (enriched by controller)
                toInteger(row[8]),     // totalStock
                (Boolean) row[6],      // topSelling
                (Boolean) row[10]);    // featured
    }

    private Map<Long, List<String>> loadImageMap(List<Long> variantIds) {
        if (variantIds.isEmpty())
            return Map.of();
        return variantRepo.findImagesByVariantIds(variantIds).stream()
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0],
                        Collectors.mapping(row -> (String) row[1], Collectors.toList())));
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null)
            return null;
        if (value instanceof BigDecimal bd)
            return bd;
        return new BigDecimal(value.toString());
    }

    private Integer toInteger(Object value) {
        if (value == null)
            return 0;
        if (value instanceof Long l)
            return l.intValue();
        if (value instanceof Integer i)
            return i;
        return ((Number) value).intValue();
    }
}
