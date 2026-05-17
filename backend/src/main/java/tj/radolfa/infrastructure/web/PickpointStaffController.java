package tj.radolfa.infrastructure.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.order.ConfirmPickpointArrivalUseCase;
import tj.radolfa.application.ports.in.order.ConfirmReturnedToWarehouseUseCase;
import tj.radolfa.application.ports.in.order.ConfirmWithDeliveryCodeUseCase;
import tj.radolfa.application.ports.in.order.GetPickpointOrdersUseCase;
import tj.radolfa.application.ports.in.order.InitiateReturnToWarehouseUseCase;
import tj.radolfa.application.ports.in.order.VerifyPickupByCodeUseCase;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.User;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter.JwtAuthenticatedUser;
import tj.radolfa.infrastructure.web.dto.PickpointOrderDto;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pickpoint")
public class PickpointStaffController {

    private final GetPickpointOrdersUseCase          getPickpointOrdersUseCase;
    private final ConfirmWithDeliveryCodeUseCase     confirmWithDeliveryCodeUseCase;
    private final ConfirmPickpointArrivalUseCase     confirmPickpointArrivalUseCase;
    private final VerifyPickupByCodeUseCase          verifyPickupByCodeUseCase;
    private final InitiateReturnToWarehouseUseCase   initiateReturnUseCase;
    private final ConfirmReturnedToWarehouseUseCase  confirmReturnedUseCase;
    private final LoadUserPort                       loadUserPort;
    private final int                                pickpointStorageDays;

    public PickpointStaffController(GetPickpointOrdersUseCase getPickpointOrdersUseCase,
                                    ConfirmWithDeliveryCodeUseCase confirmWithDeliveryCodeUseCase,
                                    ConfirmPickpointArrivalUseCase confirmPickpointArrivalUseCase,
                                    VerifyPickupByCodeUseCase verifyPickupByCodeUseCase,
                                    InitiateReturnToWarehouseUseCase initiateReturnUseCase,
                                    ConfirmReturnedToWarehouseUseCase confirmReturnedUseCase,
                                    LoadUserPort loadUserPort,
                                    @Value("${radolfa.delivery.pickpoint-storage-days:7}") int pickpointStorageDays) {
        this.getPickpointOrdersUseCase      = getPickpointOrdersUseCase;
        this.confirmWithDeliveryCodeUseCase = confirmWithDeliveryCodeUseCase;
        this.confirmPickpointArrivalUseCase = confirmPickpointArrivalUseCase;
        this.verifyPickupByCodeUseCase      = verifyPickupByCodeUseCase;
        this.initiateReturnUseCase          = initiateReturnUseCase;
        this.confirmReturnedUseCase         = confirmReturnedUseCase;
        this.loadUserPort                   = loadUserPort;
        this.pickpointStorageDays           = pickpointStorageDays;
    }

    record ConfirmRequest(@NotBlank String code) {}

    @GetMapping("/orders")
    @PreAuthorize("hasRole('PICKPOINT_STAFF')")
    public ResponseEntity<PageResponse<PickpointOrderDto>> getMyOrders(
            @RequestParam(required = false) List<OrderStatus> statuses,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {

        var result = getPickpointOrdersUseCase.execute(principal.userId(), statuses, page, size);

        var dtos = result.content().stream().map(order -> {
            User customer = loadUserPort.loadById(order.userId()).orElse(null);
            return PickpointOrderDto.from(order, customer, pickpointStorageDays);
        }).toList();

        return ResponseEntity.ok(PageResponse.from(
                new tj.radolfa.domain.model.PageResult<>(
                        dtos, result.totalElements(), result.number(), result.size(), result.last())));
    }

    @PostMapping("/orders/{orderId}/confirm")
    @PreAuthorize("hasRole('PICKPOINT_STAFF')")
    public ResponseEntity<Void> confirm(
            @PathVariable Long orderId,
            @Valid @RequestBody ConfirmRequest body,
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {
        confirmWithDeliveryCodeUseCase.execute(
                new ConfirmWithDeliveryCodeUseCase.Command(orderId, body.code()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/orders/{orderId}/confirm-arrival")
    @PreAuthorize("hasRole('PICKPOINT_STAFF')")
    public ResponseEntity<Void> confirmArrival(
            @PathVariable Long orderId,
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {
        confirmPickpointArrivalUseCase.execute(orderId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-pickup")
    @PreAuthorize("hasRole('PICKPOINT_STAFF')")
    public ResponseEntity<Void> verifyPickup(
            @Valid @RequestBody ConfirmRequest body,
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {
        verifyPickupByCodeUseCase.execute(body.code(), principal.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/orders/{orderId}/initiate-return")
    @PreAuthorize("hasRole('PICKPOINT_STAFF')")
    public ResponseEntity<Void> initiateReturn(
            @PathVariable Long orderId,
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {
        initiateReturnUseCase.execute(orderId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/orders/{orderId}/confirm-returned")
    @PreAuthorize("hasRole('PICKPOINT_STAFF')")
    public ResponseEntity<Void> confirmReturned(
            @PathVariable Long orderId,
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {
        confirmReturnedUseCase.execute(orderId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
