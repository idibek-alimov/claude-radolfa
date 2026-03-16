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
import tj.radolfa.application.ports.out.LoadColorImagesPort;
import tj.radolfa.infrastructure.persistence.entity.ProductVariantEntity;
import tj.radolfa.infrastructure.persistence.repository.ProductVariantRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

/**
 * Admin endpoint for Elasticsearch index management.
 */
@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "Search", description = "Search index management")
public class SearchController {

    private static final Logger LOG = LoggerFactory.getLogger(SearchController.class);

    private final ProductVariantRepository variantRepo;
    private final ListingIndexPort indexPort;
    private final LoadColorImagesPort loadColorImagesPort;

    public SearchController(ProductVariantRepository variantRepo,
                            ListingIndexPort indexPort,
                            LoadColorImagesPort loadColorImagesPort) {
        this.variantRepo = variantRepo;
        this.indexPort   = indexPort;
        this.loadColorImagesPort = loadColorImagesPort;
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

        int indexed = 0;
        int errors = 0;
        int pageNum = 0;
        final int pageSize = 200;
        Page<ProductVariantEntity> page;

        do {
            page = variantRepo.findAll(PageRequest.of(pageNum++, pageSize));

            for (ProductVariantEntity variant : page.getContent()) {
                try {
                    String categoryName = variant.getTemplate().getCategoryName();
                    String colorKey = variant.getAttributes().getOrDefault("Color", null);

                    List<String> images = loadColorImagesPort.findImagesByTemplateAndColor(
                            variant.getTemplate().getId(), colorKey);

                    Double price = variant.getPrice() != null
                            ? variant.getPrice().doubleValue()
                            : null;
                    int stockQty = variant.getStockQty() != null ? variant.getStockQty() : 0;

                    indexPort.index(
                            variant.getId(),
                            variant.getSeoSlug(),
                            variant.getTemplate().getName(),
                            categoryName,
                            colorKey,
                            null,       // colorHexCode not in new model
                            variant.getTemplate().getDescription(),
                            images,
                            price,
                            stockQty,
                            variant.getTemplate().isTopSelling(),
                            variant.getTemplate().isFeatured(),
                            variant.getLastSyncAt()
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
