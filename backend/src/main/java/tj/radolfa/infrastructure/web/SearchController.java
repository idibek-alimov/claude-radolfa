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
import tj.radolfa.infrastructure.persistence.entity.ListingVariantEntity;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantImageEntity;
import tj.radolfa.infrastructure.persistence.entity.SkuEntity;
import tj.radolfa.infrastructure.persistence.repository.ListingVariantRepository;

import java.math.BigDecimal;
import java.util.List;
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

    public SearchController(ListingVariantRepository variantRepo,
                            ListingIndexPort indexPort) {
        this.variantRepo = variantRepo;
        this.indexPort   = indexPort;
    }

    /**
     * Rebuild the entire listings search index from PostgreSQL.
     */
    @PostMapping("/reindex")
    @PreAuthorize("hasRole('SYSTEM')")
    @Operation(summary = "Reindex all listings",
               description = "Rebuilds the Elasticsearch listings index from PostgreSQL (SYSTEM only)")
    public ResponseEntity<ReindexResult> reindex() {
        LOG.info("[REINDEX] Starting full listings reindex");

        List<ListingVariantEntity> allVariants = variantRepo.findAll();
        int indexed = 0;
        int errors = 0;

        for (ListingVariantEntity variant : allVariants) {
            try {
                List<SkuEntity> skus = variant.getSkus();

                BigDecimal priceStart = skus.stream()
                        .map(s -> s.getSalePrice() != null ? s.getSalePrice() : s.getPrice())
                        .filter(Objects::nonNull)
                        .min(BigDecimal::compareTo)
                        .orElse(null);

                BigDecimal priceEnd = skus.stream()
                        .map(s -> s.getSalePrice() != null ? s.getSalePrice() : s.getPrice())
                        .filter(Objects::nonNull)
                        .max(BigDecimal::compareTo)
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

                indexPort.index(
                        variant.getId(),
                        variant.getSlug(),
                        variant.getProductBase().getName(),
                        category,
                        colorKey,
                        colorHexCode,
                        variant.getWebDescription(),
                        images,
                        priceStart != null ? priceStart.doubleValue() : null,
                        priceEnd != null ? priceEnd.doubleValue() : null,
                        totalStock,
                        variant.isTopSelling(),
                        variant.isFeatured(),
                        variant.getLastSyncAt()
                );
                indexed++;
            } catch (Exception e) {
                LOG.warn("[REINDEX] Failed to index variant id={}: {}",
                        variant.getId(), e.getMessage());
                errors++;
            }
        }

        LOG.info("[REINDEX] Completed -- indexed={}, errors={}", indexed, errors);
        return ResponseEntity.ok(new ReindexResult(indexed, errors));
    }

    public record ReindexResult(int indexed, int errors) {}
}
