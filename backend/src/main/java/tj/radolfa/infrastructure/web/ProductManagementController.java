package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tj.radolfa.application.ports.in.GenericUploadImageUseCase;
import tj.radolfa.application.ports.in.discount.FindCampaignsByProductUseCase;
import tj.radolfa.application.ports.in.product.AddSkuToVariantUseCase;
import tj.radolfa.application.ports.in.product.AddVariantToProductUseCase;
import tj.radolfa.application.ports.in.product.CreateProductUseCase;
import tj.radolfa.application.ports.in.product.GetProductCardUseCase;
import tj.radolfa.application.ports.in.product.ReorderVariantImagesUseCase;
import tj.radolfa.domain.model.ProductAttribute;
import tj.radolfa.application.ports.in.product.UpdateProductCategoryUseCase;
import tj.radolfa.application.ports.in.product.UpdateProductNameUseCase;
import tj.radolfa.application.ports.in.product.UpdateProductPriceUseCase;
import tj.radolfa.application.ports.in.product.UpdateProductStockUseCase;
import tj.radolfa.application.ports.in.product.UpdateSkuSizeLabelUseCase;
import tj.radolfa.application.readmodel.ProductCardDto;
import tj.radolfa.domain.exception.ImageProcessingException;
import tj.radolfa.domain.model.Money;
import tj.radolfa.infrastructure.web.dto.AddSkuRequestDto;
import tj.radolfa.infrastructure.web.dto.AddVariantRequestDto;
import tj.radolfa.infrastructure.web.dto.CreateProductRequestDto;
import tj.radolfa.infrastructure.web.dto.MessageResponseDto;
import tj.radolfa.infrastructure.web.dto.ReorderImagesRequestDto;
import tj.radolfa.infrastructure.web.dto.UpdatePriceRequestDto;
import tj.radolfa.infrastructure.web.dto.UpdateProductCategoryRequestDto;
import tj.radolfa.infrastructure.web.dto.UpdateProductNameRequestDto;
import tj.radolfa.infrastructure.web.dto.UpdateSkuSizeLabelRequestDto;
import tj.radolfa.infrastructure.web.dto.UpdateStockRequestDto;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Admin endpoints for native product management.
 * All routes require at minimum MANAGER role; price/stock mutations require
 * ADMIN.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin — Product Management", description = "Admin/Manager endpoints for native product creation and mutation")
public class ProductManagementController {

    private final CreateProductUseCase createProductUseCase;
    private final GetProductCardUseCase getProductCardUseCase;
    private final ReorderVariantImagesUseCase reorderVariantImagesUseCase;
    private final AddSkuToVariantUseCase addSkuToVariantUseCase;
    private final AddVariantToProductUseCase addVariantToProductUseCase;
    private final UpdateProductPriceUseCase updateProductPriceUseCase;
    private final UpdateProductStockUseCase updateProductStockUseCase;
    private final UpdateProductNameUseCase updateProductNameUseCase;
    private final UpdateSkuSizeLabelUseCase updateSkuSizeLabelUseCase;
    private final UpdateProductCategoryUseCase updateProductCategoryUseCase;
    private final GenericUploadImageUseCase genericUploadImageUseCase;
    private final FindCampaignsByProductUseCase findCampaignsByProductUseCase;

    public ProductManagementController(CreateProductUseCase createProductUseCase,
            GetProductCardUseCase getProductCardUseCase,
            ReorderVariantImagesUseCase reorderVariantImagesUseCase,
            AddSkuToVariantUseCase addSkuToVariantUseCase,
            AddVariantToProductUseCase addVariantToProductUseCase,
            UpdateProductPriceUseCase updateProductPriceUseCase,
            UpdateProductStockUseCase updateProductStockUseCase,
            UpdateProductNameUseCase updateProductNameUseCase,
            UpdateSkuSizeLabelUseCase updateSkuSizeLabelUseCase,
            UpdateProductCategoryUseCase updateProductCategoryUseCase,
            GenericUploadImageUseCase genericUploadImageUseCase,
            FindCampaignsByProductUseCase findCampaignsByProductUseCase) {
        this.createProductUseCase = createProductUseCase;
        this.getProductCardUseCase = getProductCardUseCase;
        this.reorderVariantImagesUseCase = reorderVariantImagesUseCase;
        this.addSkuToVariantUseCase = addSkuToVariantUseCase;
        this.addVariantToProductUseCase = addVariantToProductUseCase;
        this.updateProductPriceUseCase = updateProductPriceUseCase;
        this.updateProductStockUseCase = updateProductStockUseCase;
        this.updateProductNameUseCase = updateProductNameUseCase;
        this.updateSkuSizeLabelUseCase = updateSkuSizeLabelUseCase;
        this.updateProductCategoryUseCase = updateProductCategoryUseCase;
        this.genericUploadImageUseCase = genericUploadImageUseCase;
        this.findCampaignsByProductUseCase = findCampaignsByProductUseCase;
    }

    /**
     * POST /api/v1/admin/images/upload
     * Upload a media image without product context. Returns a permanent S3 URL.
     * Frontend calls this first; URLs are then passed into product creation
     * requests.
     * MANAGER + ADMIN.
     */
    @Operation(summary = "Upload a media image (staged upload)", description = "Processes and uploads an image to S3 at uploads/media/{uuid}.webp. "
            +
            "No product context required. Use the returned URL in product creation requests.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image uploaded successfully; body contains {url}"),
            @ApiResponse(responseCode = "400", description = "Image processing failed (corrupt file or unsupported format)"),
            @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    @PostMapping(value = "/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, String>> uploadMediaImage(
            @RequestParam("image") MultipartFile image) {

        if (image.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            String url = genericUploadImageUseCase.upload(
                    image.getInputStream(),
                    image.getOriginalFilename());
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IOException e) {
            throw new ImageProcessingException("Failed to read uploaded file: " + image.getOriginalFilename(), e);
        }
    }

    /**
     * POST /api/v1/admin/products
     * Create a new product with variants and SKUs. MANAGER + ADMIN.
     */
    @Operation(summary = "Create product", description = "Creates a full product hierarchy (ProductBase → ListingVariants → SKUs) in a single request. "
            +
            "Upload images first via POST /admin/images/upload, then pass the URLs in the variants[].images array.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created; body contains {productBaseId}"),
            @ApiResponse(responseCode = "400", description = "Validation failed or referenced entity not found (category, brand, color)"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "409", description = "Duplicate barcode or other unique constraint violation")
    })
    @PostMapping("/products")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Long>> createProduct(
            @Valid @RequestBody CreateProductRequestDto request) {

        var command = new CreateProductUseCase.Command(
                request.name(),
                request.categoryId(),
                request.brandId(),
                request.variants().stream()
                        .map(v -> new CreateProductUseCase.Command.VariantDefinition(
                                v.colorId(),
                                v.webDescription(),
                                v.attributes() == null ? List.of()
                                        : v.attributes().stream()
                                                .map(a -> new ProductAttribute(a.key(), a.values(), a.sortOrder()))
                                                .toList(),
                                v.images() == null ? List.of() : v.images(),
                                v.skus().stream()
                                        .map(s -> new CreateProductUseCase.Command.SkuDefinition(
                                                s.sizeLabel(),
                                                new Money(s.price()),
                                                s.stockQuantity()))
                                        .toList(),
                                v.isEnabled() != null && v.isEnabled(),
                                v.isActive() == null || v.isActive(),
                                v.weightKg(),
                                v.widthCm(),
                                v.heightCm(),
                                v.depthCm()))
                        .toList());

        Long productBaseId = createProductUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("productBaseId", productBaseId));
    }

    /**
     * GET /api/v1/admin/products/{productBaseId}
     * Retrieve the full admin product card (base + all variants + SKUs). MANAGER + ADMIN.
     */
    @Operation(summary = "Get admin product card",
            description = "Returns the full product card: base metadata plus every color variant "
                    + "with its images, attributes, tags and SKUs. Prices are raw (no discount/loyalty enrichment).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product card returned"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/products/{productBaseId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ProductCardDto> getProductCard(@PathVariable Long productBaseId) {
        return ResponseEntity.ok(getProductCardUseCase.execute(productBaseId));
    }

    /**
     * PATCH /api/v1/admin/listings/{variantId}/images/order
     * Persist a new image sort order for the variant. MANAGER + ADMIN.
     */
    @Operation(summary = "Reorder variant images",
            description = "Persists the new sort order of a variant's images. "
                    + "The imageIds list must exactly match the set of existing image IDs for the variant. "
                    + "MANAGER + ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image order updated"),
            @ApiResponse(responseCode = "400", description = "imageIds do not match existing images"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Variant not found")
    })
    @PatchMapping("/listings/{variantId}/images/order")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<MessageResponseDto> reorderVariantImages(
            @PathVariable Long variantId,
            @Valid @RequestBody ReorderImagesRequestDto request) {

        reorderVariantImagesUseCase.execute(variantId, request.imageIds());
        return ResponseEntity.ok(MessageResponseDto.success("Image order updated successfully."));
    }

    /**
     * POST /api/v1/admin/products/{productBaseId}/variants/{variantId}/skus
     * Add a new SKU (size entry) to an existing variant. ADMIN only.
     */
    @Operation(summary = "Add SKU to variant",
            description = "Creates a new size/price entry under the given variant. "
                    + "Auto-generates skuCode and barcode. ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "SKU created; body contains {skuId}"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Variant not found")
    })
    @PostMapping("/products/{productBaseId}/variants/{variantId}/skus")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> addSkuToVariant(
            @PathVariable Long productBaseId,
            @PathVariable Long variantId,
            @Valid @RequestBody AddSkuRequestDto request) {

        Long skuId = addSkuToVariantUseCase.execute(new AddSkuToVariantUseCase.Command(
                productBaseId,
                variantId,
                request.sizeLabel(),
                new tj.radolfa.domain.model.Money(request.price()),
                request.stockQuantity()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("skuId", skuId));
    }

    /**
     * POST /api/v1/admin/products/{productBaseId}/variants
     * Add a new empty color variant to an existing product. MANAGER + ADMIN.
     */
    @Operation(summary = "Add color variant to product",
            description = "Creates a new empty color variant (no images, no SKUs) under the given ProductBase. "
                    + "The variant starts as disabled (isEnabled=false). "
                    + "MANAGER + ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Variant created; body contains {variantId, slug}"),
            @ApiResponse(responseCode = "400", description = "Validation failed or duplicate color"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "ProductBase or color not found")
    })
    @PostMapping("/products/{productBaseId}/variants")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> addVariantToProduct(
            @PathVariable Long productBaseId,
            @Valid @RequestBody AddVariantRequestDto request) {

        AddVariantToProductUseCase.Result result = addVariantToProductUseCase.execute(
                new AddVariantToProductUseCase.Command(productBaseId, request.colorId()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("variantId", result.variantId(), "slug", result.slug()));
    }

    /**
     * PUT /api/v1/admin/skus/{skuId}/price
     * Set the price of a specific SKU. ADMIN only.
     */
    @Operation(summary = "Set SKU price", description = "Overwrites the price of a specific SKU. ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Price updated"),
            @ApiResponse(responseCode = "400", description = "Invalid price value"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "SKU not found")
    })
    @PutMapping("/skus/{skuId}/price")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponseDto> updatePrice(
            @PathVariable Long skuId,
            @Valid @RequestBody UpdatePriceRequestDto request) {

        updateProductPriceUseCase.execute(skuId, new Money(request.price()));
        return ResponseEntity.ok(MessageResponseDto.success("Price updated successfully."));
    }

    /**
     * PUT /api/v1/admin/skus/{skuId}/stock
     * Set or adjust the stock of a specific SKU. ADMIN only.
     * Body: { "quantity": 50 } — sets absolute value
     * Body: { "delta": -5 } — adjusts by delta
     */
    @Operation(summary = "Set or adjust SKU stock", description = "Pass {\"quantity\": N} to set an absolute stock value, or {\"delta\": N} to adjust by N (positive or negative). ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock updated"),
            @ApiResponse(responseCode = "400", description = "Neither quantity nor delta provided"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "SKU not found")
    })
    @PutMapping("/skus/{skuId}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponseDto> updateStock(
            @PathVariable Long skuId,
            @RequestBody UpdateStockRequestDto request) {

        if (request.quantity() != null) {
            updateProductStockUseCase.setAbsolute(skuId, request.quantity());
        } else {
            updateProductStockUseCase.adjust(skuId, request.delta());
        }
        return ResponseEntity.ok(MessageResponseDto.success("Stock updated successfully."));
    }

    /**
     * PATCH /api/v1/admin/products/{productBaseId}/name
     * Rename a product. MANAGER + ADMIN.
     */
    @Operation(summary = "Rename product", description = "Updates the display name of a product base. MANAGER + ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Name updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PatchMapping("/products/{productBaseId}/name")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<MessageResponseDto> updateProductName(
            @PathVariable Long productBaseId,
            @Valid @RequestBody UpdateProductNameRequestDto request) {

        updateProductNameUseCase.execute(productBaseId, request.name());
        return ResponseEntity.ok(MessageResponseDto.success("Product name updated successfully."));
    }

    /**
     * PATCH /api/v1/admin/skus/{skuId}/size-label
     * Update the size label of a specific SKU. MANAGER + ADMIN.
     */
    @Operation(summary = "Update SKU size label", description = "Changes the human-readable size label of a SKU (e.g. \"XL\", \"42\"). MANAGER + ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Size label updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "SKU not found")
    })
    @PatchMapping("/skus/{skuId}/size-label")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<MessageResponseDto> updateSkuSizeLabel(
            @PathVariable Long skuId,
            @Valid @RequestBody UpdateSkuSizeLabelRequestDto request) {

        updateSkuSizeLabelUseCase.execute(skuId, request.sizeLabel());
        return ResponseEntity.ok(MessageResponseDto.success("SKU size label updated successfully."));
    }

    /**
     * PATCH /api/v1/admin/products/{productBaseId}/category
     * Reassign the category of a product. MANAGER + ADMIN.
     */
    @Operation(summary = "Reassign product category", description = "Moves a product to a different category. MANAGER + ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category updated"),
            @ApiResponse(responseCode = "400", description = "Category not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PatchMapping("/products/{productBaseId}/category")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<MessageResponseDto> updateProductCategory(
            @PathVariable Long productBaseId,
            @Valid @RequestBody UpdateProductCategoryRequestDto request) {

        updateProductCategoryUseCase.execute(productBaseId, request.categoryId());
        return ResponseEntity.ok(MessageResponseDto.success("Product category updated successfully."));
    }

    @Operation(summary = "Get active campaigns for a product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Campaign list returned"),
            @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    @GetMapping("/products/{productBaseId}/campaigns")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<tj.radolfa.infrastructure.web.dto.CampaignSummaryResponse>> getProductCampaigns(
            @PathVariable Long productBaseId) {
        return ResponseEntity.ok(
                findCampaignsByProductUseCase.execute(productBaseId).stream()
                        .map(tj.radolfa.infrastructure.web.dto.CampaignSummaryResponse::fromDomain)
                        .toList()
        );
    }
}
