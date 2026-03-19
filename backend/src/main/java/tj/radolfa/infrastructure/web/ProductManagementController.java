package tj.radolfa.infrastructure.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.product.CreateProductUseCase;
import tj.radolfa.application.ports.in.product.UpdateProductPriceUseCase;
import tj.radolfa.application.ports.in.product.UpdateProductStockUseCase;
import tj.radolfa.domain.model.Money;
import tj.radolfa.infrastructure.web.dto.CreateProductRequestDto;
import tj.radolfa.infrastructure.web.dto.MessageResponseDto;
import tj.radolfa.infrastructure.web.dto.UpdatePriceRequestDto;
import tj.radolfa.infrastructure.web.dto.UpdateStockRequestDto;

import java.util.Map;

/**
 * Admin endpoints for native product management.
 * All routes require at minimum MANAGER role; price/stock mutations require ADMIN.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class ProductManagementController {

    private final CreateProductUseCase      createProductUseCase;
    private final UpdateProductPriceUseCase updateProductPriceUseCase;
    private final UpdateProductStockUseCase updateProductStockUseCase;

    public ProductManagementController(CreateProductUseCase createProductUseCase,
                                       UpdateProductPriceUseCase updateProductPriceUseCase,
                                       UpdateProductStockUseCase updateProductStockUseCase) {
        this.createProductUseCase      = createProductUseCase;
        this.updateProductPriceUseCase = updateProductPriceUseCase;
        this.updateProductStockUseCase = updateProductStockUseCase;
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
                request.colorId(),
                request.skus().stream()
                        .map(s -> new CreateProductUseCase.Command.SkuDefinition(
                                s.sizeLabel(),
                                new Money(s.price()),
                                s.stockQuantity()))
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
}
