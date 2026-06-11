package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import tj.radolfa.application.ports.in.GetCategoryUseCase;
import tj.radolfa.application.ports.in.GetListingUseCase;
import tj.radolfa.application.ports.in.UpdateListingUseCase;
import tj.radolfa.application.ports.in.UploadImageUseCase;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadRatingSummaryPort;
import tj.radolfa.domain.exception.ImageProcessingException;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.ProductAttribute;
import tj.radolfa.application.readmodel.CatalogResult;
import tj.radolfa.application.readmodel.CategoryView;
import tj.radolfa.application.readmodel.ListingQueryCriteria;
import tj.radolfa.application.readmodel.ListingSort;
import tj.radolfa.application.readmodel.ListingVariantDetailDto;
import tj.radolfa.application.readmodel.ListingVariantDto;
import tj.radolfa.infrastructure.web.dto.CatalogFacetsDto;
import tj.radolfa.infrastructure.web.dto.CatalogResponseDto;
import tj.radolfa.infrastructure.web.dto.ProductAttributeDto;
import tj.radolfa.infrastructure.web.dto.RatingSummaryResponseDto;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    private final GetListingUseCase      getListingUseCase;
    private final GetCategoryUseCase     getCategoryUseCase;
    private final UpdateListingUseCase   updateListingUseCase;
    private final UploadImageUseCase     uploadImageUseCase;
    private final TierPricingEnricher    tierPricing;
    private final LoadListingVariantPort loadListingVariantPort;
    private final LoadRatingSummaryPort  loadRatingSummaryPort;

    public ListingController(GetListingUseCase getListingUseCase,
            GetCategoryUseCase getCategoryUseCase,
            UpdateListingUseCase updateListingUseCase,
            UploadImageUseCase uploadImageUseCase,
            TierPricingEnricher tierPricing,
            LoadListingVariantPort loadListingVariantPort,
            LoadRatingSummaryPort loadRatingSummaryPort) {
        this.getListingUseCase      = getListingUseCase;
        this.getCategoryUseCase     = getCategoryUseCase;
        this.updateListingUseCase   = updateListingUseCase;
        this.uploadImageUseCase     = uploadImageUseCase;
        this.tierPricing            = tierPricing;
        this.loadListingVariantPort = loadListingVariantPort;
        this.loadRatingSummaryPort  = loadRatingSummaryPort;
    }

    @GetMapping
    @Operation(summary = "Paginated listing grid", description = "Returns colour cards with aggregated price/stock")
    public ResponseEntity<PageResponse<ListingVariantDto>> grid(
            @Parameter(description = "Page number (1-based)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Items per page") @RequestParam(defaultValue = "12") int limit) {

        return ResponseEntity.ok(PageResponse.from(tierPricing.enrich(getListingUseCase.getPage(page, limit))));
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
    public ResponseEntity<PageResponse<ListingVariantDto>> search(
            @Parameter(description = "Search query") @RequestParam String q,
            @Parameter(description = "Page number (1-based)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Items per page") @RequestParam(defaultValue = "12") int limit) {

        return ResponseEntity.ok(PageResponse.from(tierPricing.enrich(getListingUseCase.search(q, page, limit))));
    }

    @GetMapping("/catalog")
    @Operation(summary = "Catalog search", description = "Unified filter/sort/facet search backing /search and /categories/{slug}/products")
    public ResponseEntity<CatalogResponseDto> catalog(
            @Parameter(description = "Search query") @RequestParam(required = false) String q,
            @Parameter(description = "Category slug — restricts to this category and its descendants") @RequestParam(required = false) String categorySlug,
            @Parameter(description = "Page number (1-based)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Items per page") @RequestParam(defaultValue = "24") int size,
            @Parameter(description = "Inclusive minimum price") @RequestParam(required = false) BigDecimal priceMin,
            @Parameter(description = "Inclusive maximum price") @RequestParam(required = false) BigDecimal priceMax,
            @Parameter(description = "Minimum discount percentage") @RequestParam(required = false) Integer minDiscount,
            @Parameter(description = "Colour keys (repeatable)") @RequestParam(required = false) List<String> color,
            @Parameter(description = "Brand IDs (repeatable)") @RequestParam(required = false) List<Long> brand,
            @Parameter(description = "Restrict to in-stock variants") @RequestParam(defaultValue = "false") boolean inStock,
            @Parameter(description = "Sort order") @RequestParam(required = false) String sort) {

        Long categoryParentId = null;
        List<Long> categoryIds = List.of();
        if (categorySlug != null && !categorySlug.isBlank()) {
            CategoryView category = getCategoryUseCase.findBySlug(categorySlug).orElse(null);
            if (category == null) {
                return ResponseEntity.notFound().build();
            }
            categoryParentId = category.id();
            categoryIds = getCategoryUseCase.getDescendantIds(category.id());
        }

        ListingQueryCriteria criteria = new ListingQueryCriteria(
                q, categoryIds, priceMin, priceMax, minDiscount, color, brand, inStock, ListingSort.fromNullable(sort));

        CatalogResult result = getListingUseCase.searchCatalog(criteria, page, size);
        PageResult<ListingVariantDto> enriched = tierPricing.enrich(result.page());

        return ResponseEntity.ok(new CatalogResponseDto(
                PageResponse.from(enriched),
                CatalogFacetsDto.from(result.facets(), buildCategoryFacet(categoryParentId))));
    }

    /** Children of {@code parentId} in category mode, or root categories when browsing without a category. */
    private List<CategoryView> buildCategoryFacet(Long parentId) {
        return getCategoryUseCase.findAll().stream()
                .filter(c -> Objects.equals(c.parentId(), parentId))
                .toList();
    }

    @GetMapping("/autocomplete")
    @Operation(summary = "Autocomplete suggestions", description = "Product name suggestions for the search box")
    public ResponseEntity<List<String>> autocomplete(
            @Parameter(description = "Partial search text") @RequestParam String q,
            @Parameter(description = "Max suggestions") @RequestParam(defaultValue = "5") int limit) {

        return ResponseEntity.ok(getListingUseCase.autocomplete(q, limit));
    }

    @GetMapping("/{slug}/rating")
    @Tag(name = "Reviews")
    @Operation(summary = "Rating summary", description = "Aggregated star-rating and size-fit summary for a listing variant")
    @ApiResponse(responseCode = "200", description = "Rating summary (zeroed if no approved reviews yet)")
    @ApiResponse(responseCode = "404", description = "Listing not found")
    public ResponseEntity<RatingSummaryResponseDto> getRating(@PathVariable String slug) {
        return loadListingVariantPort.findBySlug(slug)
                .map(variant -> loadRatingSummaryPort.findByVariantId(variant.getId())
                        .map(s -> new RatingSummaryResponseDto(
                                s.averageRating(),
                                s.reviewCount(),
                                s.distribution(),
                                s.sizeAccurate(),
                                s.sizeRunsSmall(),
                                s.sizeRunsLarge(),
                                s.traitAggregates().stream()
                                        .map(t -> new RatingSummaryResponseDto.TraitAggregateDto(
                                                t.traitKey(),
                                                t.labelI18n(),
                                                t.inputType(),
                                                t.average(),
                                                t.count()))
                                        .toList()))
                        .orElse(RatingSummaryResponseDto.empty()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ---- Manager Operations ----

    @PutMapping("/{slug}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Update listing details", description = "Manager-enrichment: web description and attributes")
    public ResponseEntity<Void> update(
            @PathVariable String slug,
            @jakarta.validation.Valid @RequestBody UpdateListingRequest request) {

        updateListingUseCase.update(slug, new UpdateListingUseCase.UpdateListingCommand(
                request.webDescription(),
                request.attributes() == null ? null : request.attributes().stream()
                        .map(a -> new ProductAttribute(a.key(), a.values(), a.sortOrder()))
                        .toList()
        ));
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/{slug}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
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

    @DeleteMapping("/{slug}/images")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Remove image from listing", description = "Removes an image by URL")
    public ResponseEntity<Void> removeImage(
            @PathVariable String slug,
            @jakarta.validation.Valid @RequestBody ImageUrlRequest request) {

        updateListingUseCase.removeImage(slug, request.url);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{slug}/dimensions")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Update variant dimensions", description = "Sets the logistics dimensions for a colour variant. All fields are optional — pass only what changed.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dimensions updated"),
        @ApiResponse(responseCode = "404", description = "Listing not found")
    })
    public ResponseEntity<Void> updateDimensions(
            @PathVariable String slug,
            @RequestBody UpdateDimensionsRequest request) {

        updateListingUseCase.updateDimensions(slug,
                new UpdateListingUseCase.UpdateDimensionsCommand(
                        request.weightKg(), request.widthCm(), request.heightCm(), request.depthCm()));
        return ResponseEntity.ok().build();
    }

    public record UpdateListingRequest(
            @jakarta.validation.constraints.Size(max = 5000, message = "Description must not exceed 5000 characters")
            String webDescription,
            @jakarta.validation.Valid List<ProductAttributeDto> attributes) {
    }

    public record UpdateDimensionsRequest(
            Double  weightKg,
            Integer widthCm,
            Integer heightCm,
            Integer depthCm) {
    }

    public record ImageUrlRequest(
            @jakarta.validation.constraints.NotBlank(message = "Image URL is required")
            @jakarta.validation.constraints.Size(max = 2048, message = "Image URL must not exceed 2048 characters")
            @jakarta.validation.constraints.Pattern(
                    regexp = "^https://\\S+$",
                    message = "Image URL must use HTTPS and contain no spaces")
            String url) {
    }
}
