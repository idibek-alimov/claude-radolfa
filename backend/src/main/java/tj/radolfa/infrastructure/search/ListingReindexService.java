package tj.radolfa.infrastructure.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import tj.radolfa.infrastructure.persistence.adapter.DiscountEnrichmentAdapter;
import tj.radolfa.infrastructure.persistence.adapter.DiscountEnrichmentAdapter.DiscountInfo;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantEntity;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantImageEntity;
import tj.radolfa.infrastructure.persistence.entity.ProductRatingSummaryEntity;
import tj.radolfa.infrastructure.persistence.entity.SkuEntity;
import tj.radolfa.infrastructure.persistence.repository.ListingVariantRepository;
import tj.radolfa.infrastructure.persistence.repository.ProductRatingSummaryRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Reusable, batched, bulk Elasticsearch reindexer for {@code ListingDocument}s.
 *
 * <p>Builds documents from PostgreSQL (variant + SKUs + discount enrichment + rating)
 * and writes them to Elasticsearch via {@link ListingSearchRepository#saveAll}, which
 * batches into a single bulk request per page — used both by the admin "Reindex all"
 * endpoint and by targeted reindexes triggered after discount campaign changes.
 */
@Component
public class ListingReindexService {

    private static final Logger LOG = LoggerFactory.getLogger(ListingReindexService.class);
    private static final int BATCH_SIZE = 200;

    private final ListingVariantRepository variantRepo;
    private final ListingSearchRepository searchRepo;
    private final DiscountEnrichmentAdapter discountEnrichment;
    private final ProductRatingSummaryRepository ratingRepo;

    public ListingReindexService(ListingVariantRepository variantRepo,
                                 ListingSearchRepository searchRepo,
                                 DiscountEnrichmentAdapter discountEnrichment,
                                 ProductRatingSummaryRepository ratingRepo) {
        this.variantRepo = variantRepo;
        this.searchRepo = searchRepo;
        this.discountEnrichment = discountEnrichment;
        this.ratingRepo = ratingRepo;
    }

    /**
     * Rebuilds the entire listings index from PostgreSQL, paging through all
     * variants {@value #BATCH_SIZE} at a time and bulk-saving each page.
     */
    @Transactional(readOnly = true)
    public ReindexResult reindexAll() {
        LOG.info("[REINDEX] Starting full listings reindex");

        int indexed = 0;
        int errors = 0;
        int pageNum = 0;
        Page<ListingVariantEntity> page;

        do {
            page = variantRepo.findAll(PageRequest.of(pageNum++, BATCH_SIZE));
            BatchResult result = indexBatch(page.getContent());
            indexed += result.indexed();
            errors += result.errors();
        } while (page.hasNext());

        LOG.info("[REINDEX] Completed -- indexed={}, errors={}", indexed, errors);
        return new ReindexResult(indexed, errors);
    }

    /**
     * Refreshes the Elasticsearch documents for exactly the given variant ids,
     * chunked into batches of {@value #BATCH_SIZE} and bulk-saved. No-op for empty input.
     *
     * <p>Variants with no active discount get {@code discountPercentage = null}, which is
     * what allows products to drop out of the "Any discount" filter once a campaign ends.
     */
    @Transactional(readOnly = true)
    public ReindexResult reindexVariants(Collection<Long> variantIds) {
        if (variantIds == null || variantIds.isEmpty()) {
            return new ReindexResult(0, 0);
        }

        List<Long> ids = new ArrayList<>(variantIds);
        int indexed = 0;
        int errors = 0;

        for (int from = 0; from < ids.size(); from += BATCH_SIZE) {
            List<Long> chunk = ids.subList(from, Math.min(from + BATCH_SIZE, ids.size()));
            List<ListingVariantEntity> variants = variantRepo.findAllById(chunk);
            BatchResult result = indexBatch(variants);
            indexed += result.indexed();
            errors += result.errors();
        }

        LOG.info("[REINDEX] Targeted reindex completed -- requested={}, indexed={}, errors={}",
                ids.size(), indexed, errors);
        return new ReindexResult(indexed, errors);
    }

    private BatchResult indexBatch(List<ListingVariantEntity> variants) {
        if (variants.isEmpty()) return new BatchResult(0, 0);

        List<Long> variantIds = variants.stream().map(ListingVariantEntity::getId).toList();
        Map<Long, DiscountInfo> discountMap = discountEnrichment.resolveForVariantsBackground(variantIds);
        Map<Long, ProductRatingSummaryEntity> ratingMap = ratingRepo.findAllById(variantIds).stream()
                .collect(Collectors.toMap(ProductRatingSummaryEntity::getListingVariantId, r -> r));

        List<ListingDocument> docs = new ArrayList<>();
        int errors = 0;
        for (ListingVariantEntity variant : variants) {
            try {
                docs.add(buildDocument(variant, discountMap.get(variant.getId()), ratingMap.get(variant.getId())));
            } catch (Exception e) {
                LOG.warn("[REINDEX] Failed to build document for variant id={}: {}",
                        variant.getId(), e.getMessage());
                errors++;
            }
        }

        if (!docs.isEmpty()) {
            try {
                searchRepo.saveAll(docs);
            } catch (Exception e) {
                LOG.warn("[REINDEX] Bulk save failed for {} documents: {}", docs.size(), e.getMessage());
                return new BatchResult(0, errors + docs.size());
            }
        }

        return new BatchResult(docs.size(), errors);
    }

    private ListingDocument buildDocument(ListingVariantEntity variant, DiscountInfo discount,
                                          ProductRatingSummaryEntity rating) {
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
        String colorKey = variant.getColor() != null ? variant.getColor().getColorKey() : null;
        String colorHexCode = variant.getColor() != null ? variant.getColor().getHexCode() : null;

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

        Integer discountPercentage = discount != null ? discount.discountPercentage().intValue() : null;
        Double ratingAverage = rating != null && rating.getAverageRating() != null
                ? rating.getAverageRating().doubleValue() : null;

        return new ListingDocument(
                variant.getId(),
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
                variant.getProductBase().getId(),
                variant.getProductBase().getStatus() != null
                        ? variant.getProductBase().getStatus().name() : null,
                categoryId,
                brandId,
                brandName,
                discountPercentage,
                ratingAverage,
                variant.getCreatedAt()
        );
    }

    public record ReindexResult(int indexed, int errors) {}

    private record BatchResult(int indexed, int errors) {}
}
