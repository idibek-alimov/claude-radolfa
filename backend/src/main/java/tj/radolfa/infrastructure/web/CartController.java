package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.AddToCartUseCase;
import tj.radolfa.application.ports.in.ClearCartUseCase;
import tj.radolfa.application.ports.in.GetCartUseCase;
import tj.radolfa.application.ports.in.RemoveFromCartUseCase;
import tj.radolfa.application.ports.in.UpdateCartItemUseCase;
import tj.radolfa.domain.model.Cart;
import tj.radolfa.domain.model.CartItem;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter.JwtAuthenticatedUser;
import tj.radolfa.infrastructure.web.dto.AddToCartRequestDto;
import tj.radolfa.infrastructure.web.dto.CartDto;
import tj.radolfa.infrastructure.web.dto.CartItemDto;
import tj.radolfa.infrastructure.web.dto.UpdateCartItemRequestDto;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Cart", description = "Server-side cart endpoints")
public class CartController {

    private final GetCartUseCase getCartUseCase;
    private final AddToCartUseCase addToCartUseCase;
    private final UpdateCartItemUseCase updateCartItemUseCase;
    private final RemoveFromCartUseCase removeFromCartUseCase;
    private final ClearCartUseCase clearCartUseCase;

    public CartController(GetCartUseCase getCartUseCase,
                          AddToCartUseCase addToCartUseCase,
                          UpdateCartItemUseCase updateCartItemUseCase,
                          RemoveFromCartUseCase removeFromCartUseCase,
                          ClearCartUseCase clearCartUseCase) {
        this.getCartUseCase = getCartUseCase;
        this.addToCartUseCase = addToCartUseCase;
        this.updateCartItemUseCase = updateCartItemUseCase;
        this.removeFromCartUseCase = removeFromCartUseCase;
        this.clearCartUseCase = clearCartUseCase;
    }

    @GetMapping
    @Operation(summary = "Get the current user's cart")
    public ResponseEntity<CartDto> getCart(@AuthenticationPrincipal JwtAuthenticatedUser user) {
        Cart cart = getCartUseCase.execute(user.userId());
        return ResponseEntity.ok(toDto(cart));
    }

    @PostMapping("/items")
    @Operation(summary = "Add an item to the cart")
    public ResponseEntity<CartDto> addToCart(
            @AuthenticationPrincipal JwtAuthenticatedUser user,
            @Valid @RequestBody AddToCartRequestDto request) {
        Cart cart = addToCartUseCase.execute(user.userId(), request.skuId(), request.quantity());
        return ResponseEntity.ok(toDto(cart));
    }

    @PutMapping("/items/{skuId}")
    @Operation(summary = "Update the quantity of a cart item")
    public ResponseEntity<CartDto> updateItem(
            @AuthenticationPrincipal JwtAuthenticatedUser user,
            @PathVariable Long skuId,
            @Valid @RequestBody UpdateCartItemRequestDto request) {
        Cart cart = updateCartItemUseCase.execute(user.userId(), skuId, request.quantity());
        return ResponseEntity.ok(toDto(cart));
    }

    @DeleteMapping("/items/{skuId}")
    @Operation(summary = "Remove a specific item from the cart")
    public ResponseEntity<CartDto> removeItem(
            @AuthenticationPrincipal JwtAuthenticatedUser user,
            @PathVariable Long skuId) {
        Cart cart = removeFromCartUseCase.execute(user.userId(), skuId);
        return ResponseEntity.ok(toDto(cart));
    }

    @DeleteMapping
    @Operation(summary = "Clear the entire cart")
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal JwtAuthenticatedUser user) {
        clearCartUseCase.execute(user.userId());
        return ResponseEntity.noContent().build();
    }

    // ---- Private mapping helpers (no MapStruct — pure DTO assembly from domain) ----

    private CartDto toDto(Cart cart) {
        List<CartItemDto> itemDtos = cart.getItems().stream()
                .map(this::toItemDto)
                .toList();
        return new CartDto(
                cart.getUserId(),
                itemDtos,
                cart.subtotal().amount(),
                itemDtos.size());
    }

    private CartItemDto toItemDto(CartItem item) {
        return new CartItemDto(
                item.getSkuId(),
                item.getListingSlug(),
                item.getProductName(),
                item.getSizeLabel(),
                item.getImageUrl(),
                item.getPriceSnapshot(),
                item.getQuantity(),
                item.itemSubtotal().amount());
    }
}
