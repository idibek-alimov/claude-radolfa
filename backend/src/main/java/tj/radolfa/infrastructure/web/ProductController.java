package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.CreateProductUseCase;
import tj.radolfa.application.ports.in.DeleteProductUseCase;
import tj.radolfa.application.ports.in.UpdateProductUseCase;
import tj.radolfa.application.ports.out.LoadProductPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.Product;
import tj.radolfa.infrastructure.web.dto.CreateProductRequestDto;
import tj.radolfa.infrastructure.web.dto.ProductDto;
import tj.radolfa.infrastructure.web.dto.UpdateProductRequestDto;

import java.util.List;

/**
 * REST controller for product read operations.
 *
 * <p>
 * All endpoints are PUBLIC (no authentication required).
 * </p>
 *
 * <p>
 * <b>Note:</b> Products can only be modified via:
 * <ul>
 * <li>ERP Sync (SYSTEM role) - for price, name, stock</li>
 * <li>Image upload (MANAGER role) - for images</li>
 * <li>Description update (MANAGER role) - for webDescription</li>
 * <li>New Manager API - full CRUD</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Product catalog operations")
public class ProductController {

    private final LoadProductPort loadProductPort;
    private final CreateProductUseCase createProductUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final DeleteProductUseCase deleteProductUseCase;

    public ProductController(LoadProductPort loadProductPort,
            CreateProductUseCase createProductUseCase,
            UpdateProductUseCase updateProductUseCase,
            DeleteProductUseCase deleteProductUseCase) {
        this.loadProductPort = loadProductPort;
        this.createProductUseCase = createProductUseCase;
        this.updateProductUseCase = updateProductUseCase;
        this.deleteProductUseCase = deleteProductUseCase;
    }

    /**
     * Paginated response wrapper for frontend compatibility.
     */
    public record PaginatedProducts(
            List<ProductDto> products,
            int total,
            int page,
            boolean hasMore) {
    }

    /**
     * Get all products (paginated).
     */
    @GetMapping
    @Operation(summary = "List all products", description = "Returns paginated products in the catalog")
    public ResponseEntity<PaginatedProducts> getAllProducts(
            @Parameter(description = "Page number (1-based)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Items per page") @RequestParam(defaultValue = "12") int limit,
            @Parameter(description = "Search by name or ERP ID") @RequestParam(required = false) String search) {

        PageResult<Product> result = loadProductPort.loadPage(page, limit, search);

        List<ProductDto> dtos = result.items().stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(new PaginatedProducts(
                dtos, (int) result.totalElements(), result.page(), result.hasMore()));
    }

    /**
     * Get top-selling products.
     */
    @GetMapping("/top-selling")
    @Operation(summary = "List top-selling products", description = "Returns products marked as top-selling")
    public ResponseEntity<List<ProductDto>> getTopSellingProducts() {
        List<ProductDto> products = loadProductPort.loadTopSelling().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(products);
    }

    /**
     * Get a single product by ERP ID.
     */
    @GetMapping("/{erpId}")
    @Operation(summary = "Get product by ERP ID", description = "Returns a single product by its ERP identifier")
    public ResponseEntity<ProductDto> getProductByErpId(@PathVariable String erpId) {
        return loadProductPort.load(erpId)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER') or hasRole('SYSTEM')")
    @Operation(summary = "Create product", description = "Create a new product (Manager/System only)")
    public ResponseEntity<ProductDto> createProduct(@RequestBody CreateProductRequestDto request) {
        Product product = createProductUseCase.execute(
                request.erpId(),
                request.name(),
                request.price(),
                request.stock(),
                request.webDescription(),
                request.topSelling(),
                request.images());
        return ResponseEntity.ok(toDto(product));
    }

    @PutMapping("/{erpId}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('SYSTEM')")
    @Operation(summary = "Update product", description = "Update an existing product (Manager/System only)")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable String erpId,
            @RequestBody UpdateProductRequestDto request) {
        Product product = updateProductUseCase.execute(
                erpId,
                request.name(),
                request.price(),
                request.stock(),
                request.webDescription(),
                request.topSelling(),
                request.images());
        return ResponseEntity.ok(toDto(product));
    }

    @DeleteMapping("/{erpId}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('SYSTEM')")
    @Operation(summary = "Delete product", description = "Delete a product by ERP ID (Manager/System only)")
    public ResponseEntity<Void> deleteProduct(@PathVariable String erpId) {
        deleteProductUseCase.execute(erpId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Map domain Product to ProductDto.
     */
    private ProductDto toDto(Product product) {
        return new ProductDto(
                product.getId(),
                product.getErpId(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getWebDescription(),
                product.isTopSelling(),
                product.getImages(),
                product.getLastErpSyncAt());
    }
}
