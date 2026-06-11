package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.LoadHomeCollectionsPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.infrastructure.persistence.adapter.DiscountEnrichmentAdapter.DiscountInfo;
import tj.radolfa.infrastructure.persistence.entity.ProductRatingSummaryEntity;
import tj.radolfa.infrastructure.persistence.repository.ListingVariantRepository;
import tj.radolfa.infrastructure.persistence.repository.OrderItemRepository;
import tj.radolfa.infrastructure.persistence.repository.ProductBaseRepository;
import tj.radolfa.infrastructure.persistence.repository.ProductRatingSummaryRepository;
import tj.radolfa.infrastructure.persistence.repository.SellerRepository;
import tj.radolfa.infrastructure.persistence.repository.SkuRepository;
import tj.radolfa.application.readmodel.ListingVariantDto;
import tj.radolfa.application.readmodel.ListingVariantDto.TagView;
import tj.radolfa.application.readmodel.SkuDto;

import java.util.Comparator;
import java.util.HashMap;
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

    private static final String FEATURED_TAG_NAME = "featured";

    private final ListingVariantRepository variantRepo;
    private final SkuRepository skuRepo;
    private final DiscountEnrichmentAdapter discountEnrichment;
    private final OrderItemRepository orderItemRepo;
    private final ProductRatingSummaryRepository ratingRepo;
    private final ProductBaseRepository productBaseRepo;
    private final SellerRepository sellerRepo;

    public HomeCollectionsAdapter(ListingVariantRepository variantRepo,
                                  SkuRepository skuRepo,
                                  DiscountEnrichmentAdapter discountEnrichment,
                                  OrderItemRepository orderItemRepo,
                                  ProductRatingSummaryRepository ratingRepo,
                                  ProductBaseRepository productBaseRepo,
                                  SellerRepository sellerRepo) {
        this.variantRepo = variantRepo;
        this.skuRepo = skuRepo;
        this.discountEnrichment = discountEnrichment;
        this.orderItemRepo = orderItemRepo;
        this.ratingRepo = ratingRepo;
        this.productBaseRepo = productBaseRepo;
        this.sellerRepo = sellerRepo;
    }

    // ---- Homepage preview (limited, no pagination metadata) ----

    @Override
    public List<ListingVariantDto> loadNewArrivals(int limit) {
        return toGridDtos(variantRepo.findNewArrivalsGrid(PageRequest.of(0, limit)).getContent());
    }

    @Override
    public List<ListingVariantDto> loadTopSellers(int limit) {
        List<Long> rankedIds = orderItemRepo.findTopSellingVariantIds(PageRequest.of(0, limit));
        if (rankedIds.isEmpty()) return List.of();
        List<Object[]> rows = variantRepo.findGridByVariantIds(rankedIds, PageRequest.of(0, rankedIds.size())).getContent();
        return sortByRankedIds(toGridDtos(rows), rankedIds);
    }

    @Override
    public PageResult<ListingVariantDto> loadTopSellersPage(int page, int limit) {
        List<Long> rankedIds = orderItemRepo.findTopSellingVariantIds(PageRequest.of(page - 1, limit));
        if (rankedIds.isEmpty()) {
            return new PageResult<>(List.of(), 0, page, limit, true);
        }
        List<Object[]> rows = variantRepo.findGridByVariantIds(rankedIds, PageRequest.of(0, rankedIds.size())).getContent();
        List<ListingVariantDto> content = sortByRankedIds(toGridDtos(rows), rankedIds);
        long total = orderItemRepo.countTopSellingVariants();
        return new PageResult<>(content, total, page, limit, (long) page * limit >= total);
    }

    // ---- Paginated "View All" pages ----

    @Override
    public PageResult<ListingVariantDto> loadFeaturedPage(int page, int limit) {
        return toPageResult(variantRepo.findGridByTagName(FEATURED_TAG_NAME, PageRequest.of(page - 1, limit)), page, limit);
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
        List<Long> productBaseIds = rows.stream()
                .map(row -> (Long) row[11])
                .distinct()
                .toList();

        Map<Long, List<String>> imageMap = ListingGridRowMapper.loadImageMap(variantIds, variantRepo);
        Map<Long, DiscountInfo> discountMap = discountEnrichment.resolveForVariants(variantIds);
        Map<Long, List<SkuDto>> skuMap = ListingGridRowMapper.loadSkuMap(variantIds, skuRepo);
        Map<Long, List<TagView>> tagMap = ListingGridRowMapper.loadTagMap(variantIds, variantRepo);
        Map<Long, ProductRatingSummaryEntity> ratingMap = ListingGridRowMapper.loadRatingMap(variantIds, ratingRepo);
        Map<Long, String> sellerMap = ListingGridRowMapper.loadSellerNameMap(productBaseIds, productBaseRepo, sellerRepo);
        Map<Long, ListingGridRowMapper.BrandRef> brandMap = ListingGridRowMapper.loadBrandMap(productBaseIds, productBaseRepo);

        return rows.stream()
                .map(row -> ListingGridRowMapper.toGridDto(row, imageMap, discountMap, skuMap, tagMap, ratingMap, sellerMap, brandMap))
                .toList();
    }

    private List<ListingVariantDto> sortByRankedIds(List<ListingVariantDto> dtos, List<Long> rankedIds) {
        Map<Long, Integer> rank = new HashMap<>();
        for (int i = 0; i < rankedIds.size(); i++) rank.put(rankedIds.get(i), i);
        return dtos.stream()
                .sorted(Comparator.comparingInt(d -> rank.getOrDefault(d.variantId(), Integer.MAX_VALUE)))
                .toList();
    }
}
