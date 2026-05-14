package tj.radolfa.infrastructure.web.dto;

/**
 * Request body for updating a cart item's quantity.
 * A quantity ≤ 0 removes the item from the cart.
 */
public record UpdateCartItemRequestDto(int quantity) {}
