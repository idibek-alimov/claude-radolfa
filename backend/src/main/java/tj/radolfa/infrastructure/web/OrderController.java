package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.GetMyOrdersUseCase;
import tj.radolfa.application.ports.in.order.CancelOrderUseCase;
import tj.radolfa.application.ports.in.order.CheckoutUseCase;
import tj.radolfa.application.ports.in.order.UpdateOrderStatusUseCase;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter.JwtAuthenticatedUser;
import tj.radolfa.infrastructure.web.dto.CheckoutRequestDto;
import tj.radolfa.infrastructure.web.dto.CheckoutResponseDto;
import tj.radolfa.infrastructure.web.dto.OrderDto;
import tj.radolfa.infrastructure.web.dto.OrderItemDto;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order endpoints")
public class OrderController {

    private final GetMyOrdersUseCase      getMyOrdersUseCase;
    private final CheckoutUseCase         checkoutUseCase;
    private final CancelOrderUseCase      cancelOrderUseCase;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;

    public OrderController(GetMyOrdersUseCase getMyOrdersUseCase,
                           CheckoutUseCase checkoutUseCase,
                           CancelOrderUseCase cancelOrderUseCase,
                           UpdateOrderStatusUseCase updateOrderStatusUseCase) {
        this.getMyOrdersUseCase      = getMyOrdersUseCase;
        this.checkoutUseCase         = checkoutUseCase;
        this.cancelOrderUseCase      = cancelOrderUseCase;
        this.updateOrderStatusUseCase = updateOrderStatusUseCase;
    }

    @GetMapping("/my-orders")
    @Operation(summary = "Get my order history")
    public ResponseEntity<List<OrderDto>> getMyOrders(@AuthenticationPrincipal JwtAuthenticatedUser user) {
        List<Order> orders = getMyOrdersUseCase.execute(user.userId());
        return ResponseEntity.ok(orders.stream().map(this::toDto).toList());
    }

    @PostMapping("/checkout")
    @Operation(summary = "Checkout: convert active cart to a PENDING order")
    public ResponseEntity<CheckoutResponseDto> checkout(
            @AuthenticationPrincipal JwtAuthenticatedUser user,
            @RequestBody CheckoutRequestDto request) {

        CheckoutUseCase.Command command = new CheckoutUseCase.Command(
                user.userId(),
                request.loyaltyPointsToRedeem(),
                request.notes());

        CheckoutUseCase.Result result = checkoutUseCase.execute(command);
        return ResponseEntity.ok(CheckoutResponseDto.from(result));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order (USER: own PENDING only; ADMIN: any non-final)")
    public ResponseEntity<Void> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtAuthenticatedUser user,
            @RequestBody(required = false) Map<String, String> body) {

        String reason = body != null ? body.get("reason") : null;
        cancelOrderUseCase.execute(id, user.userId(), reason);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update order status (ADMIN only: PENDING→PAID→SHIPPED→DELIVERED)")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String statusStr = body.get("status");
        if (statusStr == null || statusStr.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        updateOrderStatusUseCase.execute(id, newStatus);
        return ResponseEntity.noContent().build();
    }

    private OrderDto toDto(Order order) {
        return new OrderDto(
                order.id(),
                order.status().name(),
                order.totalAmount().amount(),
                order.items().stream()
                        .map(item -> new OrderItemDto(
                                item.getProductName(),
                                item.getQuantity(),
                                item.getPrice().amount(),
                                item.getSkuId(),
                                item.getListingVariantId()))
                        .toList(),
                order.createdAt());
    }
}
