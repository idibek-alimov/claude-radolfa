package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tj.radolfa.application.ports.out.ListingIndexPort;
import tj.radolfa.infrastructure.persistence.adapter.DiscountEnrichmentAdapter;
import tj.radolfa.infrastructure.persistence.adapter.DiscountEnrichmentAdapter.DiscountInfo;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantEntity;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantImageEntity;
import tj.radolfa.infrastructure.persistence.entity.ProductRatingSummaryEntity;
import tj.radolfa.infrastructure.persistence.entity.SkuEntity;
import tj.radolfa.infrastructure.persistence.repository.ListingVariantRepository;
import tj.radolfa.infrastructure.persistence.repository.ProductRatingSummaryRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Admin endpoint for Elasticsearch index management.
 */
@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "Search", description = "Search index management")
public class SearchController {

    private static final Logger LOG = LoggerFactory.getLogger(SearchController.class);

    private final ListingVariantRepository variantRepo;
    private final ListingIndexPort indexPort;
    private final DiscountEnrichmentAdapter discountEnrichment;
    private final ProductRatingSummaryRepository ratingRepo;

    public SearchController(ListingVariantRepository variantRepo,
                            ListingIndexPort indexPort,
                            DiscountEnrichmentAdapter discountEnrichment,
                            ProductRatingSummaryRepository ratingRepo) {
        this.variantRepo = variantRepo;
        this.indexPort   = indexPort;
        this.discountEnrichment = discountEnrichment;
        this.ratingRepo = ratingRepo;
    }

    /**
     * Rebuild the entire listings search index from PostgreSQL.
     */
    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reindex all listings",
               description = "Rebuilds the Elasticsearch listings index from PostgreSQL (ADMIN only)")
    public ResponseEntity<ReindexResult> reindex() {
        LOG.info("[REINDEX] Starting full listings reindex");

        int indexed = 0;
        int errors = 0;
        int pageNum = 0;
        final int pageSize = 200;
        Page<ListingVariantEntity> page;

        do {
            page = variantRepo.findAll(PageRequest.of(pageNum++, pageSize));

        List<Long> pageVariantIds = page.getContent().stream()
                .map(ListingVariantEntity::getId)
                .toList();
        Map<Long, DiscountInfo> discountMap = discountEnrichment.resolveForVariants(pageVariantIds);
        Map<Long, ProductRatingSummaryEntity> ratingMap = ratingRepo.findAllById(pageVariantIds).stream()
                .collect(java.util.stream.Collectors.toMap(ProductRatingSummaryEntity::getListingVariantId, r -> r));

        for (ListingVariantEntity variant : page.getContent()) {
            try {
                List<SkuEntity> skus = variant.getSkus();

                // ES stores original price only; discounts are enriched at read time
                BigDecimal price = skus.stream()
                        .map(SkuEntity::getOriginalPrice)
                        .filter(Objects::nonNull)
                        .min(BigDecimal::compareTo)
                        .orElse(null);

                int totalStock = skus.stream()
                        .mapToInt(s -> s.getStockQuantity() != null ? s.getStockQuantity() : 0)
                        .sum();

                List<String> images = variant.getImages().stream()
                        .map(ListingVariantImageEntity::getImageUrl)
                        .toList();

                String category = variant.getProductBase().getCategory() != null
                        ? variant.getProductBase().getCategory().getName()
                        : null;
                String colorKey = variant.getColor() != null
                        ? variant.getColor().getColorKey() : null;
                String colorHexCode = variant.getColor() != null
                        ? variant.getColor().getHexCode() : null;

                List<String> skuCodes = skus.stream()
                        .map(SkuEntity::getSkuCode)
                        .filter(Objects::nonNull)
                        .toList();

                Long categoryId = variant.getProductBase().getCategory() != null
                        ? variant.getProductBase().getCategory().getId() : null;
                Long brandId = variant.getProductBase().getBrand() != null
                        ? variant.getProductBase().getBrand().getId() : null;
                String brandName = variant.getProductBase().getBrand() != null
                        ? variant.getProductBase().getBrand().getName() : null;

                DiscountInfo discount = discountMap.get(variant.getId());
                Integer discountPercentage = discount != null
                        ? discount.discountPercentage().intValue() : null;

                ProductRatingSummaryEntity rating = ratingMap.get(variant.getId());
                Double ratingAverage = rating != null && rating.getAverageRating() != null
                        ? rating.getAverageRating().doubleValue() : null;

                indexPort.index(
                        variant.getId(),
                        variant.getProductBase().getId(),
                        variant.getSlug(),
                        variant.getProductBase().getName(),
                        category,
                        colorKey,
                        colorHexCode,
                        variant.getWebDescription(),
                        images,
                        price != null ? price.doubleValue() : null,
                        totalStock,
                        variant.getLastSyncAt(),
                        variant.getProductCode(),
                        skuCodes,
                        variant.getProductBase().getStatus() != null
                                ? variant.getProductBase().getStatus().name() : null,
                        categoryId,
                        brandId,
                        brandName,
                        discountPercentage,
                        ratingAverage,
                        variant.getCreatedAt()
                );
                indexed++;
            } catch (Exception e) {
                LOG.warn("[REINDEX] Failed to index variant id={}: {}",
                        variant.getId(), e.getMessage());
                errors++;
            }
        }

        } while (page.hasNext());

        LOG.info("[REINDEX] Completed -- indexed={}, errors={}", indexed, errors);
        return ResponseEntity.ok(new ReindexResult(indexed, errors));
    }

    public record ReindexResult(int indexed, int errors) {}
}
