package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tj.radolfa.application.ports.in.GenericUploadImageUseCase;
import tj.radolfa.application.ports.in.product.CreateProductUseCase;
import tj.radolfa.domain.model.ProductAttribute;
import tj.radolfa.application.ports.in.product.UpdateProductCategoryUseCase;
import tj.radolfa.application.ports.in.product.UpdateProductNameUseCase;
import tj.radolfa.application.ports.in.product.UpdateProductPriceUseCase;
import tj.radolfa.application.ports.in.product.UpdateProductStockUseCase;
import tj.radolfa.application.ports.in.product.UpdateSkuSizeLabelUseCase;
import tj.radolfa.domain.exception.ImageProcessingException;
import tj.radolfa.domain.model.Money;
import tj.radolfa.infrastructure.web.dto.CreateProductRequestDto;
import tj.radolfa.infrastructure.web.dto.MessageResponseDto;
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
 * All routes require at minimum MANAGER role; price/stock mutations require ADMIN.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class ProductManagementController {

    private final CreateProductUseCase         createProductUseCase;
    private final UpdateProductPriceUseCase    updateProductPriceUseCase;
    private final UpdateProductStockUseCase    updateProductStockUseCase;
    private final UpdateProductNameUseCase     updateProductNameUseCase;
    private final UpdateSkuSizeLabelUseCase    updateSkuSizeLabelUseCase;
    private final UpdateProductCategoryUseCase updateProductCategoryUseCase;
    private final GenericUploadImageUseCase    genericUploadImageUseCase;

    public ProductManagementController(CreateProductUseCase createProductUseCase,
                                       UpdateProductPriceUseCase updateProductPriceUseCase,
                                       UpdateProductStockUseCase updateProductStockUseCase,
                                       UpdateProductNameUseCase updateProductNameUseCase,
                                       UpdateSkuSizeLabelUseCase updateSkuSizeLabelUseCase,
                                       UpdateProductCategoryUseCase updateProductCategoryUseCase,
                                       GenericUploadImageUseCase genericUploadImageUseCase) {
        this.createProductUseCase         = createProductUseCase;
        this.updateProductPriceUseCase    = updateProductPriceUseCase;
        this.updateProductStockUseCase    = updateProductStockUseCase;
        this.updateProductNameUseCase     = updateProductNameUseCase;
        this.updateSkuSizeLabelUseCase    = updateSkuSizeLabelUseCase;
        this.updateProductCategoryUseCase = updateProductCategoryUseCase;
        this.genericUploadImageUseCase    = genericUploadImageUseCase;
    }

    /**
     * POST /api/v1/admin/images/upload
     * Upload a media image without product context. Returns a permanent S3 URL.
     * Frontend calls this first; URLs are then passed into product creation requests.
     * MANAGER + ADMIN.
     */
    @Operation(summary = "Upload a media image (staged upload)",
               description = "Processes and uploads an image to S3 at uploads/media/{uuid}.webp. " +
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
                                v.attributes() == null ? List.of() : v.attributes().stream()
                                        .map(a -> new ProductAttribute(a.key(), a.value(), a.sortOrder()))
                                        .toList(),
                                v.images() == null ? List.of() : v.images(),
                                v.skus().stream()
                                        .map(s -> new CreateProductUseCase.Command.SkuDefinition(
                                                s.sizeLabel(),
                                                new Money(s.price()),
                                                s.stockQuantity(),
                                                s.barcode(),
                                                s.weightKg(),
                                                s.widthCm(),
                                                s.heightCm(),
                                                s.depthCm()))
                                        .toList()
                        ))
                        .toList()
        );

        Long productBaseId = createProductUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("productBaseId", productBaseId));
    }

    /**
     * PUT /api/v1/admin/skus/{skuId}/price
     * Set the price of a specific SKU. ADMIN only.
     */
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
     * Body: { "quantity": 50 }  — sets absolute value
     * Body: { "delta": -5 }     — adjusts by delta
     */
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
    @PatchMapping("/products/{productBaseId}/category")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<MessageResponseDto> updateProductCategory(
            @PathVariable Long productBaseId,
            @Valid @RequestBody UpdateProductCategoryRequestDto request) {

        updateProductCategoryUseCase.execute(productBaseId, request.categoryId());
        return ResponseEntity.ok(MessageResponseDto.success("Product category updated successfully."));
    }
}
