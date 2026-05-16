package tj.radolfa.infrastructure.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.order.ConfirmWithDeliveryCodeUseCase;
import tj.radolfa.application.ports.in.order.GetPickpointOrdersUseCase;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.User;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter.JwtAuthenticatedUser;
import tj.radolfa.infrastructure.web.dto.PickpointOrderDto;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pickpoint")
public class PickpointStaffController {

    private final GetPickpointOrdersUseCase      getPickpointOrdersUseCase;
    private final ConfirmWithDeliveryCodeUseCase confirmWithDeliveryCodeUseCase;
    private final LoadUserPort                   loadUserPort;
    private final int                            pickpointStorageDays;

    public PickpointStaffController(GetPickpointOrdersUseCase getPickpointOrdersUseCase,
                                    ConfirmWithDeliveryCodeUseCase confirmWithDeliveryCodeUseCase,
                                    LoadUserPort loadUserPort,
                                    @Value("${radolfa.delivery.pickpoint-storage-days:7}") int pickpointStorageDays) {
        this.getPickpointOrdersUseCase    = getPickpointOrdersUseCase;
        this.confirmWithDeliveryCodeUseCase = confirmWithDeliveryCodeUseCase;
        this.loadUserPort                 = loadUserPort;
        this.pickpointStorageDays         = pickpointStorageDays;
    }

    record ConfirmRequest(@NotBlank String code) {}

    @GetMapping("/orders")
    @PreAuthorize("hasRole('PICKPOINT_STAFF')")
    public ResponseEntity<List<PickpointOrderDto>> getMyOrders(
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {
        List<Order> orders = getPickpointOrdersUseCase.execute(principal.userId());

        List<PickpointOrderDto> dtos = orders.stream().map(order -> {
            User customer = loadUserPort.loadById(order.userId()).orElse(null);
            return PickpointOrderDto.from(order, customer, pickpointStorageDays);
        }).toList();

        return ResponseEntity.ok(dtos);
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
}
