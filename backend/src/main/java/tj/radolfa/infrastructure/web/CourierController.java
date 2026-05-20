package tj.radolfa.infrastructure.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.order.ConfirmRecallReceivedUseCase;
import tj.radolfa.application.ports.in.order.ConfirmWithDeliveryCodeUseCase;
import tj.radolfa.application.ports.in.order.GetCourierOrdersUseCase;
import tj.radolfa.application.ports.in.order.MarkDeliveryAttemptedUseCase;
import tj.radolfa.application.ports.in.order.MarkOutForDeliveryUseCase;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.domain.model.DeliveryAttemptReason;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.Sku;
import tj.radolfa.domain.model.User;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter.JwtAuthenticatedUser;
import tj.radolfa.infrastructure.web.dto.CourierOrderDto;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/courier")
public class CourierController {

    private final GetCourierOrdersUseCase       getCourierOrdersUseCase;
    private final ConfirmRecallReceivedUseCase   confirmRecallReceivedUseCase;
    private final MarkOutForDeliveryUseCase      markOutForDeliveryUseCase;
    private final ConfirmWithDeliveryCodeUseCase confirmWithDeliveryCodeUseCase;
    private final MarkDeliveryAttemptedUseCase   markDeliveryAttemptedUseCase;
    private final LoadUserPort                   loadUserPort;
    private final LoadSkuPort                    loadSkuPort;

    public CourierController(GetCourierOrdersUseCase getCourierOrdersUseCase,
                             ConfirmRecallReceivedUseCase confirmRecallReceivedUseCase,
                             MarkOutForDeliveryUseCase markOutForDeliveryUseCase,
                             ConfirmWithDeliveryCodeUseCase confirmWithDeliveryCodeUseCase,
                             MarkDeliveryAttemptedUseCase markDeliveryAttemptedUseCase,
                             LoadUserPort loadUserPort,
                             LoadSkuPort loadSkuPort) {
        this.getCourierOrdersUseCase      = getCourierOrdersUseCase;
        this.confirmRecallReceivedUseCase = confirmRecallReceivedUseCase;
        this.markOutForDeliveryUseCase    = markOutForDeliveryUseCase;
        this.confirmWithDeliveryCodeUseCase = confirmWithDeliveryCodeUseCase;
        this.markDeliveryAttemptedUseCase = markDeliveryAttemptedUseCase;
        this.loadUserPort                 = loadUserPort;
        this.loadSkuPort                  = loadSkuPort;
    }

    private static final List<OrderStatus> DEFAULT_STATUSES = List.of(
            OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERY_ATTEMPTED);

    record ConfirmRequest(@NotBlank String code) {}

    record AttemptRequest(@NotNull DeliveryAttemptReason reason, String photoUrl) {}

    @GetMapping("/orders")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<PageResponse<CourierOrderDto>> getMyOrders(
            @RequestParam(required = false) List<OrderStatus> status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {

        List<OrderStatus> statuses = (status == null || status.isEmpty())
                ? DEFAULT_STATUSES
                : status;

        PageResult<Order> result = getCourierOrdersUseCase.execute(principal.userId(), statuses, page, size);

        Set<Long> skuIds = result.content().stream()
                .flatMap(o -> o.items() == null ? java.util.stream.Stream.empty() : o.items().stream())
                .map(OrderItem::getSkuId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, Sku> skuMap = loadSkuPort.findAllByIdsAsMap(skuIds);

        List<CourierOrderDto> dtos = result.content().stream().map(order -> {
            User customer = loadUserPort.loadById(order.userId()).orElse(null);
            return CourierOrderDto.from(order, customer, skuMap);
        }).toList();

        return ResponseEntity.ok(PageResponse.from(
                new PageResult<>(dtos, result.totalElements(), result.number(), result.size(), result.last())));
    }

    @PostMapping("/orders/{orderId}/collect")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<Void> collect(
            @PathVariable Long orderId,
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {
        markOutForDeliveryUseCase.execute(orderId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/orders/{orderId}/confirm")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<Void> confirm(
            @PathVariable Long orderId,
            @Valid @RequestBody ConfirmRequest body,
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {
        confirmWithDeliveryCodeUseCase.execute(
                new ConfirmWithDeliveryCodeUseCase.Command(orderId, body.code()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/orders/{orderId}/attempt")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<Void> attempt(
            @PathVariable Long orderId,
            @Valid @RequestBody AttemptRequest body,
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {
        markDeliveryAttemptedUseCase.execute(new MarkDeliveryAttemptedUseCase.Command(
                orderId, principal.userId(), body.reason(), body.photoUrl()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/orders/{orderId}/confirm-recall")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<Void> confirmRecall(
            @PathVariable Long orderId,
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {
        confirmRecallReceivedUseCase.execute(orderId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
