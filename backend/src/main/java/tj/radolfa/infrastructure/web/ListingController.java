package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tj.radolfa.application.ports.in.GetListingUseCase;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.infrastructure.web.dto.ListingVariantDetailDto;
import tj.radolfa.infrastructure.web.dto.ListingVariantDto;

import java.util.List;

/**
 * Public storefront API for the 3-tier product hierarchy.
 *
 * <p>All endpoints are unauthenticated — the storefront is public.
 */
@RestController
@RequestMapping("/api/v1/listings")
@Tag(name = "Listings", description = "Storefront listing variant operations")
public class ListingController {

    private final GetListingUseCase getListingUseCase;

    public ListingController(GetListingUseCase getListingUseCase) {
        this.getListingUseCase = getListingUseCase;
    }

    @GetMapping
    @Operation(summary = "Paginated listing grid",
               description = "Returns colour cards with aggregated price/stock")
    public ResponseEntity<PageResult<ListingVariantDto>> grid(
            @Parameter(description = "Page number (1-based)")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Items per page")
            @RequestParam(defaultValue = "12") int limit) {

        return ResponseEntity.ok(getListingUseCase.getPage(page, limit));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Listing detail",
               description = "Full variant detail with SKUs and sibling colour swatches")
    public ResponseEntity<ListingVariantDetailDto> detail(@PathVariable String slug) {
        return getListingUseCase.getBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    @Operation(summary = "Search listings",
               description = "Full-text fuzzy search (ES with SQL fallback)")
    public ResponseEntity<PageResult<ListingVariantDto>> search(
            @Parameter(description = "Search query") @RequestParam String q,
            @Parameter(description = "Page number (1-based)")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Items per page")
            @RequestParam(defaultValue = "12") int limit) {

        return ResponseEntity.ok(getListingUseCase.search(q, page, limit));
    }

    @GetMapping("/autocomplete")
    @Operation(summary = "Autocomplete suggestions",
               description = "Product name suggestions for the search box")
    public ResponseEntity<List<String>> autocomplete(
            @Parameter(description = "Partial search text") @RequestParam String q,
            @Parameter(description = "Max suggestions")
            @RequestParam(defaultValue = "5") int limit) {

        return ResponseEntity.ok(getListingUseCase.autocomplete(q, limit));
    }
}
