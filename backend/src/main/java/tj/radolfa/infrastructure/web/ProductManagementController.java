package tj.radolfa.infrastructure.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.product.AddSkuUseCase;
import tj.radolfa.application.ports.in.product.CreateProductUseCase;
import tj.radolfa.application.ports.in.product.UpdateProductCategoryUseCase;
import tj.radolfa.application.ports.in.product.UpdateProductNameUseCase;
import tj.radolfa.application.ports.in.product.UpdateProductPriceUseCase;
import tj.radolfa.application.ports.in.product.UpdateProductStockUseCase;
import tj.radolfa.application.ports.in.product.UpdateSkuSizeLabelUseCase;
import tj.radolfa.domain.model.Money;
import tj.radolfa.infrastructure.web.dto.CreateProductRequestDto;

import java.util.Map;
import tj.radolfa.infrastructure.web.dto.CreateProductResponseDto;
import tj.radolfa.infrastructure.web.dto.MessageResponseDto;
import tj.radolfa.infrastructure.web.dto.UpdatePriceRequestDto;
import tj.radolfa.infrastructure.web.dto.UpdateProductCategoryRequestDto;
import tj.radolfa.infrastructure.web.dto.UpdateProductNameRequestDto;
import tj.radolfa.infrastructure.web.dto.UpdateSkuSizeLabelRequestDto;
import tj.radolfa.infrastructure.web.dto.UpdateStockRequestDto;

/**
 * Admin endpoints for native product management.
 * All routes require at minimum MANAGER role; price/stock mutations require ADMIN.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class ProductManagementController {

    private final AddSkuUseCase                addSkuUseCase;
    private final CreateProductUseCase         createProductUseCase;
    private final UpdateProductPriceUseCase    updateProductPriceUseCase;
    private final UpdateProductStockUseCase    updateProductStockUseCase;
    private final UpdateProductNameUseCase     updateProductNameUseCase;
    private final UpdateSkuSizeLabelUseCase    updateSkuSizeLabelUseCase;
    private final UpdateProductCategoryUseCase updateProductCategoryUseCase;

    public ProductManagementController(AddSkuUseCase addSkuUseCase,
                                       CreateProductUseCase createProductUseCase,
                                       UpdateProductPriceUseCase updateProductPriceUseCase,
                                       UpdateProductStockUseCase updateProductStockUseCase,
                                       UpdateProductNameUseCase updateProductNameUseCase,
                                       UpdateSkuSizeLabelUseCase updateSkuSizeLabelUseCase,
                                       UpdateProductCategoryUseCase updateProductCategoryUseCase) {
        this.addSkuUseCase            = addSkuUseCase;
        this.createProductUseCase         = createProductUseCase;
        this.updateProductPriceUseCase    = updateProductPriceUseCase;
        this.updateProductStockUseCase    = updateProductStockUseCase;
        this.updateProductNameUseCase     = updateProductNameUseCase;
        this.updateSkuSizeLabelUseCase    = updateSkuSizeLabelUseCase;
        this.updateProductCategoryUseCase = updateProductCategoryUseCase;
    }

    /**
     * POST /api/v1/admin/products
     * Create a new product with variants and SKUs. MANAGER + ADMIN.
     */
    @PostMapping("/products")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<CreateProductResponseDto> createProduct(
            @Valid @RequestBody CreateProductRequestDto request) {

        var command = new CreateProductUseCase.Command(
                request.name(),
                request.categoryId(),
                request.colorId(),
                request.webDescription(),
                request.skus().stream()
                        .map(s -> new CreateProductUseCase.Command.SkuDefinition(
                                s.sizeLabel(),
                                new Money(s.price()),
                                s.stockQuantity()))
                        .toList(),
                request.attributes() != null
                        ? request.attributes().stream()
                            .map((a) -> new CreateProductUseCase.Command.AttributeDefinition(
                                    a.key(), a.value(), 0))
                            .toList()
                        : null
        );

        CreateProductUseCase.Result result = createProductUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateProductResponseDto(result.productBaseId(), result.variantId(), result.slug()));
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

    /**
     * POST /api/v1/admin/skus
     * Add a new SKU to an existing variant. ADMIN only (controls price + stock).
     */
    @PostMapping("/skus")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> addSku(
            @Valid @RequestBody AddSkuRequestDto request) {

        var command = new AddSkuUseCase.Command(
                request.variantId(),
                request.sizeLabel(),
                new Money(request.price()),
                request.stockQuantity()
        );
        AddSkuUseCase.Result result = addSkuUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("skuId", result.skuId(), "skuCode", result.skuCode()));
    }

    public record AddSkuRequestDto(
            @jakarta.validation.constraints.NotNull(message = "variantId is required")
            Long variantId,
            @jakarta.validation.constraints.NotBlank(message = "sizeLabel is required")
            String sizeLabel,
            @jakarta.validation.constraints.NotNull(message = "price is required")
            @jakarta.validation.constraints.PositiveOrZero(message = "price must be ≥ 0")
            java.math.BigDecimal price,
            @jakarta.validation.constraints.PositiveOrZero(message = "stockQuantity must be ≥ 0")
            int stockQuantity
    ) {}
}
