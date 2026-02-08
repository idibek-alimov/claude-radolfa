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
import tj.radolfa.application.ports.out.ElasticsearchProductIndexer;
import tj.radolfa.application.ports.out.LoadProductPort;
import tj.radolfa.domain.model.Product;

import java.util.List;

/**
 * Admin endpoint for Elasticsearch index management.
 */
@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "Search", description = "Search index management")
public class SearchController {

    private static final Logger LOG = LoggerFactory.getLogger(SearchController.class);

    private final LoadProductPort loadProductPort;
    private final ElasticsearchProductIndexer indexer;

    public SearchController(LoadProductPort loadProductPort,
                            ElasticsearchProductIndexer indexer) {
        this.loadProductPort = loadProductPort;
        this.indexer = indexer;
    }

    /**
     * Rebuild the entire search index from PostgreSQL.
     * Required for initial deployment and recovery.
     */
    @PostMapping("/reindex")
    @PreAuthorize("hasRole('SYSTEM')")
    @Operation(summary = "Reindex all products", description = "Rebuilds the Elasticsearch index from PostgreSQL (SYSTEM only)")
    public ResponseEntity<ReindexResult> reindex() {
        LOG.info("[REINDEX] Starting full reindex");

        List<Product> allProducts = loadProductPort.loadAll();
        int indexed = 0;
        int errors = 0;

        for (Product product : allProducts) {
            try {
                indexer.index(product);
                indexed++;
            } catch (Exception e) {
                LOG.warn("[REINDEX] Failed to index erpId={}: {}", product.getErpId(), e.getMessage());
                errors++;
            }
        }

        LOG.info("[REINDEX] Completed -- indexed={}, errors={}", indexed, errors);
        return ResponseEntity.ok(new ReindexResult(indexed, errors));
    }

    public record ReindexResult(int indexed, int errors) {}
}
