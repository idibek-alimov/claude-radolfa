package tj.radolfa.infrastructure.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.order.ConfirmWithDeliveryCodeUseCase;
import tj.radolfa.application.ports.in.order.GetCourierOrdersUseCase;
import tj.radolfa.application.ports.in.order.MarkDeliveryAttemptedUseCase;
import tj.radolfa.application.ports.in.order.MarkOutForDeliveryUseCase;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.domain.model.DeliveryAttemptReason;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
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
    private final MarkOutForDeliveryUseCase      markOutForDeliveryUseCase;
    private final ConfirmWithDeliveryCodeUseCase confirmWithDeliveryCodeUseCase;
    private final MarkDeliveryAttemptedUseCase   markDeliveryAttemptedUseCase;
    private final LoadUserPort                   loadUserPort;
    private final LoadSkuPort                    loadSkuPort;

    public CourierController(GetCourierOrdersUseCase getCourierOrdersUseCase,
                             MarkOutForDeliveryUseCase markOutForDeliveryUseCase,
                             ConfirmWithDeliveryCodeUseCase confirmWithDeliveryCodeUseCase,
                             MarkDeliveryAttemptedUseCase markDeliveryAttemptedUseCase,
                             LoadUserPort loadUserPort,
                             LoadSkuPort loadSkuPort) {
        this.getCourierOrdersUseCase      = getCourierOrdersUseCase;
        this.markOutForDeliveryUseCase    = markOutForDeliveryUseCase;
        this.confirmWithDeliveryCodeUseCase = confirmWithDeliveryCodeUseCase;
        this.markDeliveryAttemptedUseCase = markDeliveryAttemptedUseCase;
        this.loadUserPort                 = loadUserPort;
        this.loadSkuPort                  = loadSkuPort;
    }

    record ConfirmRequest(@NotBlank String code) {}

    record AttemptRequest(@NotNull DeliveryAttemptReason reason, String photoUrl) {}

    @GetMapping("/orders")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<List<CourierOrderDto>> getMyOrders(
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {
        List<Order> orders = getCourierOrdersUseCase.execute(principal.userId());

        Set<Long> skuIds = orders.stream()
                .flatMap(o -> o.items() == null ? java.util.stream.Stream.empty() : o.items().stream())
                .map(OrderItem::getSkuId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, Sku> skuMap = loadSkuPort.findAllByIdsAsMap(skuIds);

        List<CourierOrderDto> dtos = orders.stream().map(order -> {
            User customer = loadUserPort.loadById(order.userId()).orElse(null);
            return CourierOrderDto.from(order, customer, skuMap);
        }).toList();

        return ResponseEntity.ok(dtos);
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
}
