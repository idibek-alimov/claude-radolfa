package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.out.LoadProductPort;
import tj.radolfa.domain.model.Product;
import tj.radolfa.infrastructure.web.dto.ProductDto;

import java.util.List;

/**
 * REST controller for product read operations.
 *
 * <p>All endpoints are PUBLIC (no authentication required).</p>
 *
 * <p><b>Note:</b> Products can only be modified via:
 * <ul>
 *   <li>ERP Sync (SYSTEM role) - for price, name, stock</li>
 *   <li>Image upload (MANAGER role) - for images</li>
 *   <li>Description update (MANAGER role) - for webDescription</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Product catalog operations")
public class ProductController {

    private final LoadProductPort loadProductPort;

    public ProductController(LoadProductPort loadProductPort) {
        this.loadProductPort = loadProductPort;
    }

    /**
     * Paginated response wrapper for frontend compatibility.
     */
    public record PaginatedProducts(
            List<ProductDto> products,
            int total,
            int page,
            boolean hasMore
    ) {}

    /**
     * Get all products (paginated).
     */
    @GetMapping
    @Operation(summary = "List all products", description = "Returns paginated products in the catalog")
    public ResponseEntity<PaginatedProducts> getAllProducts(
            @Parameter(description = "Page number (1-based)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Items per page") @RequestParam(defaultValue = "12") int limit
    ) {
        List<Product> allProducts = loadProductPort.loadAll();
        int total = allProducts.size();

        // Simple in-memory pagination
        int startIndex = (page - 1) * limit;
        int endIndex = Math.min(startIndex + limit, total);

        List<ProductDto> pageProducts = allProducts.stream()
                .skip(startIndex)
                .limit(limit)
                .map(this::toDto)
                .toList();

        boolean hasMore = endIndex < total;

        return ResponseEntity.ok(new PaginatedProducts(pageProducts, total, page, hasMore));
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
                product.getLastErpSyncAt()
        );
    }
}
