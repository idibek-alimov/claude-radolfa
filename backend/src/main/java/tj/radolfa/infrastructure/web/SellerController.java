package tj.radolfa.infrastructure.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.product.CreateProductUseCase;
import tj.radolfa.application.ports.in.product.ListAdminProductsUseCase;
import tj.radolfa.application.ports.in.seller.GetMySellerProfileUseCase;
import tj.radolfa.application.readmodel.AdminProductRow;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.ProductAttribute;
import tj.radolfa.domain.model.ProductStatus;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter.JwtAuthenticatedUser;
import tj.radolfa.infrastructure.web.dto.AdminProductRowDto;
import tj.radolfa.infrastructure.web.dto.CreateProductRequestDto;
import tj.radolfa.infrastructure.web.dto.SellerDto;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/seller")
public class SellerController {

    private final GetMySellerProfileUseCase  getMySellerProfileUseCase;
    private final CreateProductUseCase       createProductUseCase;
    private final ListAdminProductsUseCase   listAdminProductsUseCase;

    public SellerController(GetMySellerProfileUseCase getMySellerProfileUseCase,
                            CreateProductUseCase createProductUseCase,
                            ListAdminProductsUseCase listAdminProductsUseCase) {
        this.getMySellerProfileUseCase = getMySellerProfileUseCase;
        this.createProductUseCase      = createProductUseCase;
        this.listAdminProductsUseCase  = listAdminProductsUseCase;
    }

    /** GET /api/v1/seller/me — return the caller's seller profile. */
    @GetMapping("/me")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<SellerDto> getMyProfile(
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {
        var seller = getMySellerProfileUseCase.execute(principal.userId());
        return ResponseEntity.ok(SellerDto.from(seller));
    }

    /**
     * POST /api/v1/seller/me/products
     * Create a product owned by this seller. sellerId is resolved server-side — never
     * taken from the request body — so it cannot be spoofed.
     */
    @PostMapping("/me/products")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Map<String, Long>> createMyProduct(
            @Valid @RequestBody CreateProductRequestDto request,
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {

        Long sellerId = getMySellerProfileUseCase.execute(principal.userId()).id();

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
                        .toList(),
                sellerId); // injected from the authenticated principal — not from the request body

        Long productBaseId = createProductUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("productBaseId", productBaseId));
    }

    /**
     * GET /api/v1/seller/me/products
     * List only the caller's own products (server-side WHERE seller_id = :me).
     * Supports the same status/search/page/size params as the admin listing.
     */
    @GetMapping("/me/products")
    @PreAuthorize("hasRole('SELLER')")
    public PageResponse<AdminProductRowDto> listMyProducts(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {

        Long sellerId = getMySellerProfileUseCase.execute(principal.userId()).id();
        PageResult<AdminProductRow> result =
                listAdminProductsUseCase.execute(status, search, page, size, sellerId);
        return PageResponse.from(result.map(AdminProductRowDto::from));
    }
}
