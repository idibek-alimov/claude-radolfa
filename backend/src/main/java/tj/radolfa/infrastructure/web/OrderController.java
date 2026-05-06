package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.GetMyOrdersUseCase;
import tj.radolfa.application.ports.in.order.CancelOrderUseCase;
import tj.radolfa.application.ports.in.order.CheckoutUseCase;
import tj.radolfa.application.ports.in.order.UpdateOrderStatusUseCase;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadPickpointPort;
import tj.radolfa.application.ports.out.LoadReviewPort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.Pickpoint;
import tj.radolfa.domain.model.Sku;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter.JwtAuthenticatedUser;
import tj.radolfa.infrastructure.web.dto.CheckoutRequestDto;
import tj.radolfa.infrastructure.web.dto.CheckoutResponseDto;
import tj.radolfa.infrastructure.web.dto.OrderDto;
import tj.radolfa.infrastructure.web.dto.OrderItemDto;
import tj.radolfa.infrastructure.web.dto.UpdateOrderStatusRequest;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order endpoints")
public class OrderController {

    private final GetMyOrdersUseCase       getMyOrdersUseCase;
    private final CheckoutUseCase          checkoutUseCase;
    private final CancelOrderUseCase       cancelOrderUseCase;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private final LoadListingVariantPort   loadListingVariantPort;
    private final LoadSkuPort              loadSkuPort;
    private final LoadReviewPort           loadReviewPort;
    private final LoadPickpointPort        loadPickpointPort;

    public OrderController(GetMyOrdersUseCase getMyOrdersUseCase,
                           CheckoutUseCase checkoutUseCase,
                           CancelOrderUseCase cancelOrderUseCase,
                           UpdateOrderStatusUseCase updateOrderStatusUseCase,
                           LoadListingVariantPort loadListingVariantPort,
                           LoadSkuPort loadSkuPort,
                           LoadReviewPort loadReviewPort,
                           LoadPickpointPort loadPickpointPort) {
        this.getMyOrdersUseCase       = getMyOrdersUseCase;
        this.checkoutUseCase          = checkoutUseCase;
        this.cancelOrderUseCase       = cancelOrderUseCase;
        this.updateOrderStatusUseCase = updateOrderStatusUseCase;
        this.loadListingVariantPort   = loadListingVariantPort;
        this.loadSkuPort              = loadSkuPort;
        this.loadReviewPort           = loadReviewPort;
        this.loadPickpointPort        = loadPickpointPort;
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
                request.notes(),
                request.deliveryType(),
                request.address(),
                request.preferredTimeWindow(),
                request.pickpointId());

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
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update order status (ADMIN only: PENDING→PAID→SHIPPED→DELIVERED). courierName required for HOME→SHIPPED.")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateOrderStatusRequest body) {

        if (body.status() == null || body.status().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(body.status().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        updateOrderStatusUseCase.execute(new UpdateOrderStatusUseCase.Command(
                id, newStatus, body.courierName(), body.trackingNumber(), body.estimatedDeliveryDate()));
        return ResponseEntity.noContent().build();
    }

    private OrderDto toDto(Order order) {
        List<Long> variantIds = order.items().stream()
                .map(OrderItem::getListingVariantId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, ListingVariant> variantMap = variantIds.isEmpty()
                ? Map.of()
                : loadListingVariantPort.findVariantsByIds(variantIds);

        List<Long> skuIds = order.items().stream()
                .map(OrderItem::getSkuId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Sku> skuMap = skuIds.isEmpty()
                ? Map.of()
                : loadSkuPort.findAllByIdsAsMap(skuIds);

        Pickpoint pickpoint = order.pickpointId() != null
                ? loadPickpointPort.findById(order.pickpointId()).orElse(null)
                : null;

        return new OrderDto(
                order.id(),
                order.status().name(),
                order.totalAmount().amount(),
                order.items().stream()
                        .map(item -> {
                            ListingVariant variant = item.getListingVariantId() != null
                                    ? variantMap.get(item.getListingVariantId())
                                    : null;
                            String imageUrl = (variant != null && !variant.getImages().isEmpty())
                                    ? variant.getImages().get(0)
                                    : null;
                            String slug = variant != null ? variant.getSlug() : null;
                            Sku sku = item.getSkuId() != null ? skuMap.get(item.getSkuId()) : null;
                            String sizeLabel = sku != null ? sku.getSizeLabel() : null;
                            boolean hasReviewed = item.getListingVariantId() != null
                                    && loadReviewPort.existsByOrderAndVariant(order.id(), item.getListingVariantId());
                            return new OrderItemDto(
                                    item.getProductName(),
                                    item.getQuantity(),
                                    item.getPrice().amount(),
                                    item.getSkuId(),
                                    item.getListingVariantId(),
                                    imageUrl,
                                    item.getSkuCode(),
                                    sizeLabel,
                                    slug,
                                    hasReviewed);
                        })
                        .toList(),
                order.createdAt(),
                order.loyaltyPointsRedeemed(),
                order.loyaltyPointsAwarded(),
                order.deliveryType() != null ? order.deliveryType().name() : null,
                order.deliveryAddress(),
                order.preferredTimeWindow(),
                order.pickpointId(),
                pickpoint != null ? pickpoint.name()    : null,
                pickpoint != null ? pickpoint.address() : null,
                order.courierName(),
                order.trackingNumber(),
                order.estimatedDeliveryDate());
    }
}
