package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.CreateOrderUseCase;
import tj.radolfa.application.ports.in.GetMyOrdersUseCase;
import tj.radolfa.domain.model.Order;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter.JwtAuthenticatedUser;
import tj.radolfa.infrastructure.web.dto.CreateOrderRequestDto;
import tj.radolfa.infrastructure.web.dto.OrderDto;
import tj.radolfa.infrastructure.web.dto.OrderItemDto;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

    private final GetMyOrdersUseCase getMyOrdersUseCase;
    private final CreateOrderUseCase createOrderUseCase;

    public OrderController(GetMyOrdersUseCase getMyOrdersUseCase, CreateOrderUseCase createOrderUseCase) {
        this.getMyOrdersUseCase = getMyOrdersUseCase;
        this.createOrderUseCase = createOrderUseCase;
    }

    @GetMapping("/my-orders")
    @Operation(summary = "Get my order history")
    public ResponseEntity<List<OrderDto>> getMyOrders(@AuthenticationPrincipal JwtAuthenticatedUser user) {
        List<Order> orders = getMyOrdersUseCase.execute(user.userId());
        return ResponseEntity.ok(orders.stream().map(this::toDto).toList());
    }

    @PostMapping
    @Operation(summary = "Create a new order")
    public ResponseEntity<OrderDto> createOrder(@AuthenticationPrincipal JwtAuthenticatedUser user,
            @RequestBody CreateOrderRequestDto request) {
        Order order = createOrderUseCase.execute(user.userId(), request.items());
        return ResponseEntity.ok(toDto(order));
    }

    private OrderDto toDto(Order order) {
        return new OrderDto(
                order.id(),
                order.status().name(),
                order.totalAmount(),
                order.items().stream()
                        .map(item -> new OrderItemDto(
                                item.productName(),
                                item.quantity(),
                                item.priceAtPurchase()))
                        .toList(),
                order.createdAt());
    }
}
