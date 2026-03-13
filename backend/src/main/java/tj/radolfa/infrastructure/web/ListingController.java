package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import tj.radolfa.application.ports.in.GetListingUseCase;
import tj.radolfa.application.ports.in.UploadImageUseCase;
import tj.radolfa.domain.exception.ImageProcessingException;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.infrastructure.web.dto.ListingVariantDetailDto;
import tj.radolfa.infrastructure.web.dto.ListingVariantDto;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Public storefront API for the 3-tier product hierarchy.
 *
 * <p>
 * All endpoints are unauthenticated — the storefront is public.
 */
@RestController
@RequestMapping("/api/v1/listings")
@Tag(name = "Listings", description = "Storefront listing variant operations")
public class ListingController {

    private final GetListingUseCase getListingUseCase;
    private final tj.radolfa.application.ports.in.UpdateListingUseCase updateListingUseCase;
    private final UploadImageUseCase uploadImageUseCase;
    private final TierPricingEnricher tierPricing;

    public ListingController(GetListingUseCase getListingUseCase,
            tj.radolfa.application.ports.in.UpdateListingUseCase updateListingUseCase,
            UploadImageUseCase uploadImageUseCase,
            TierPricingEnricher tierPricing) {
        this.getListingUseCase = getListingUseCase;
        this.updateListingUseCase = updateListingUseCase;
        this.uploadImageUseCase = uploadImageUseCase;
        this.tierPricing = tierPricing;
    }

    @GetMapping
    @Operation(summary = "Paginated listing grid", description = "Returns colour cards with aggregated price/stock")
    public ResponseEntity<PageResult<ListingVariantDto>> grid(
            @Parameter(description = "Page number (1-based)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Items per page") @RequestParam(defaultValue = "12") int limit) {

        return ResponseEntity.ok(tierPricing.enrich(getListingUseCase.getPage(page, limit)));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Listing detail", description = "Full variant detail with SKUs and sibling colour swatches")
    public ResponseEntity<ListingVariantDetailDto> detail(@PathVariable String slug) {
        return getListingUseCase.getBySlug(slug)
                .map(tierPricing::enrich)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    @Operation(summary = "Search listings", description = "Full-text fuzzy search (ES with SQL fallback)")
    public ResponseEntity<PageResult<ListingVariantDto>> search(
            @Parameter(description = "Search query") @RequestParam String q,
            @Parameter(description = "Page number (1-based)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Items per page") @RequestParam(defaultValue = "12") int limit) {

        return ResponseEntity.ok(tierPricing.enrich(getListingUseCase.search(q, page, limit)));
    }

    @GetMapping("/autocomplete")
    @Operation(summary = "Autocomplete suggestions", description = "Product name suggestions for the search box")
    public ResponseEntity<List<String>> autocomplete(
            @Parameter(description = "Partial search text") @RequestParam String q,
            @Parameter(description = "Max suggestions") @RequestParam(defaultValue = "5") int limit) {

        return ResponseEntity.ok(getListingUseCase.autocomplete(q, limit));
    }

    // ---- Manager Operations (Should be secured in production) ----

    @org.springframework.web.bind.annotation.PutMapping("/{slug}")
    @Operation(summary = "Update listing details", description = "Manager-enrichment: description, top-selling status")
    public ResponseEntity<Void> update(
            @PathVariable String slug,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody UpdateListingRequest request) {

        updateListingUseCase.update(slug, new tj.radolfa.application.ports.in.UpdateListingUseCase.UpdateListingCommand(
                request.webDescription,
                request.topSelling,
                request.featured,
                null // images updated separately
        ));
        return ResponseEntity.ok().build();
    }

    @org.springframework.web.bind.annotation.PostMapping(value = "/{slug}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload image to listing", description = "Processes and uploads an image file to S3, then appends the URL to the gallery")
    public ResponseEntity<Map<String, String>> addImage(
            @PathVariable String slug,
            @RequestParam("image") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            String url = uploadImageUseCase.upload(slug, file.getInputStream(), file.getOriginalFilename());
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IOException e) {
            throw new ImageProcessingException("Failed to read uploaded file", e);
        }
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{slug}/images")
    @Operation(summary = "Remove image from listing", description = "Removes an image by URL")
    public ResponseEntity<Void> removeImage(
            @PathVariable String slug,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody ImageUrlRequest request) {

        updateListingUseCase.removeImage(slug, request.url);
        return ResponseEntity.ok().build();
    }

    public record UpdateListingRequest(String webDescription, Boolean topSelling, Boolean featured) {
    }

    public record ImageUrlRequest(
            @jakarta.validation.constraints.NotBlank(message = "Image URL is required")
            @jakarta.validation.constraints.Size(max = 2048, message = "Image URL must not exceed 2048 characters")
            @jakarta.validation.constraints.Pattern(
                    regexp = "^https://.+",
                    message = "Image URL must use HTTPS")
            String url) {
    }
}
