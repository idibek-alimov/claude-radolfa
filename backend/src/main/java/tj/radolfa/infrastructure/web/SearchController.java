package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tj.radolfa.infrastructure.search.ListingReindexService;
import tj.radolfa.infrastructure.search.ListingReindexService.ReindexResult;

/**
 * Admin endpoint for Elasticsearch index management.
 */
@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "Search", description = "Search index management")
public class SearchController {

    private final ListingReindexService reindexService;

    public SearchController(ListingReindexService reindexService) {
        this.reindexService = reindexService;
    }

    /**
     * Rebuild the entire listings search index from PostgreSQL.
     */
    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reindex all listings",
               description = "Rebuilds the Elasticsearch listings index from PostgreSQL (ADMIN only)")
    public ResponseEntity<ReindexResult> reindex() {
        return ResponseEntity.ok(reindexService.reindexAll());
    }
}
