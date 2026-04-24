package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.cart.*;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter.JwtAuthenticatedUser;
import tj.radolfa.infrastructure.web.dto.AddToCartRequestDto;
import tj.radolfa.infrastructure.web.dto.ApplyCouponRequestDto;
import tj.radolfa.infrastructure.web.dto.ApplyCouponResponseDto;
import tj.radolfa.infrastructure.web.dto.CartDto;
import tj.radolfa.infrastructure.web.dto.UpdateCartItemRequestDto;

@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Cart", description = "Shopping cart management")
public class CartController {

    private final GetCartUseCase                  getCartUseCase;
    private final AddToCartUseCase                addToCartUseCase;
    private final UpdateCartItemQuantityUseCase   updateCartItemQuantityUseCase;
    private final RemoveFromCartUseCase           removeFromCartUseCase;
    private final ClearCartUseCase                clearCartUseCase;
    private final ApplyCouponUseCase              applyCouponUseCase;
    private final RemoveCouponUseCase             removeCouponUseCase;
    private final boolean                         couponsEnabled;

    public CartController(GetCartUseCase getCartUseCase,
                          AddToCartUseCase addToCartUseCase,
                          UpdateCartItemQuantityUseCase updateCartItemQuantityUseCase,
                          RemoveFromCartUseCase removeFromCartUseCase,
                          ClearCartUseCase clearCartUseCase,
                          ApplyCouponUseCase applyCouponUseCase,
                          RemoveCouponUseCase removeCouponUseCase,
                          @Value("${radolfa.discount.coupons.enabled:true}") boolean couponsEnabled) {
        this.getCartUseCase                = getCartUseCase;
        this.addToCartUseCase              = addToCartUseCase;
        this.updateCartItemQuantityUseCase = updateCartItemQuantityUseCase;
        this.removeFromCartUseCase         = removeFromCartUseCase;
        this.clearCartUseCase              = clearCartUseCase;
        this.applyCouponUseCase            = applyCouponUseCase;
        this.removeCouponUseCase           = removeCouponUseCase;
        this.couponsEnabled                = couponsEnabled;
    }

    @GetMapping
    @Operation(summary = "Get my active cart")
    public ResponseEntity<CartDto> getCart(@AuthenticationPrincipal JwtAuthenticatedUser user) {
        return ResponseEntity.ok(CartDto.fromView(getCartUseCase.execute(user.userId())));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart (merges quantity if SKU already present)")
    public ResponseEntity<CartDto> addItem(
            @AuthenticationPrincipal JwtAuthenticatedUser user,
            @Valid @RequestBody AddToCartRequestDto request) {
        addToCartUseCase.execute(user.userId(), request.skuId(), request.quantity());
        return ResponseEntity.ok(CartDto.fromView(getCartUseCase.execute(user.userId())));
    }

    @PutMapping("/items/{skuId}")
    @Operation(summary = "Update item quantity (quantity ≤ 0 removes the item)")
    public ResponseEntity<CartDto> updateItem(
            @AuthenticationPrincipal JwtAuthenticatedUser user,
            @PathVariable Long skuId,
            @Valid @RequestBody UpdateCartItemRequestDto request) {
        updateCartItemQuantityUseCase.execute(user.userId(), skuId, request.quantity());
        return ResponseEntity.ok(CartDto.fromView(getCartUseCase.execute(user.userId())));
    }

    @DeleteMapping("/items/{skuId}")
    @Operation(summary = "Remove a specific item from cart")
    public ResponseEntity<CartDto> removeItem(
            @AuthenticationPrincipal JwtAuthenticatedUser user,
            @PathVariable Long skuId) {
        removeFromCartUseCase.execute(user.userId(), skuId);
        return ResponseEntity.ok(CartDto.fromView(getCartUseCase.execute(user.userId())));
    }

    @DeleteMapping
    @Operation(summary = "Clear all items from cart")
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal JwtAuthenticatedUser user) {
        clearCartUseCase.execute(user.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/apply-coupon")
    @Operation(summary = "Apply a coupon code to the cart")
    public ResponseEntity<ApplyCouponResponseDto> applyCoupon(
            @AuthenticationPrincipal JwtAuthenticatedUser user,
            @Valid @RequestBody ApplyCouponRequestDto request) {
        if (!couponsEnabled) return ResponseEntity.notFound().build();
        ApplyCouponUseCase.Result r = applyCouponUseCase.execute(user.userId(), request.couponCode());
        return ResponseEntity.ok(new ApplyCouponResponseDto(
                r.valid(), r.discountId(), r.affectedSkus(), r.invalidReason(),
                r.cart() != null ? CartDto.fromView(r.cart()) : null));
    }

    @DeleteMapping("/coupon")
    @Operation(summary = "Remove the applied coupon from the cart")
    public ResponseEntity<CartDto> removeCoupon(@AuthenticationPrincipal JwtAuthenticatedUser user) {
        if (!couponsEnabled) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(CartDto.fromView(removeCouponUseCase.execute(user.userId())));
    }
}
