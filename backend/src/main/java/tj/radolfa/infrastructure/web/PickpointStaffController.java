package tj.radolfa.infrastructure.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.order.ConfirmCustomerReturnSentUseCase;
import tj.radolfa.application.ports.in.order.ConfirmPickpointArrivalUseCase;
import tj.radolfa.application.ports.in.order.ConfirmReturnedToWarehouseUseCase;
import tj.radolfa.application.ports.in.order.ConfirmWithDeliveryCodeUseCase;
import tj.radolfa.application.ports.in.order.GetPickpointCustomerReturnsUseCase;
import tj.radolfa.application.ports.in.order.GetPickpointOrdersUseCase;
import tj.radolfa.application.ports.in.order.InitiateReturnToWarehouseUseCase;
import tj.radolfa.application.ports.in.order.LookUpOrderForReturnUseCase;
import tj.radolfa.application.ports.in.order.ReceiveCustomerReturnUseCase;
import tj.radolfa.application.ports.in.order.VerifyPickupByCodeUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.domain.model.CustomerReturnStatus;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.User;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter.JwtAuthenticatedUser;
import tj.radolfa.infrastructure.web.dto.CreateCustomerReturnRequestDto;
import tj.radolfa.infrastructure.web.dto.CustomerReturnDto;
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
    private final LookUpOrderForReturnUseCase        lookUpOrderForReturnUseCase;
    private final GetPickpointCustomerReturnsUseCase getPickpointCustomerReturnsUseCase;
    private final ReceiveCustomerReturnUseCase       receiveCustomerReturnUseCase;
    private final ConfirmCustomerReturnSentUseCase   confirmCustomerReturnSentUseCase;
    private final LoadOrderPort                      loadOrderPort;
    private final LoadUserPort                       loadUserPort;
    private final int                                pickpointStorageDays;

    public PickpointStaffController(GetPickpointOrdersUseCase getPickpointOrdersUseCase,
                                    ConfirmWithDeliveryCodeUseCase confirmWithDeliveryCodeUseCase,
                                    ConfirmPickpointArrivalUseCase confirmPickpointArrivalUseCase,
                                    VerifyPickupByCodeUseCase verifyPickupByCodeUseCase,
                                    InitiateReturnToWarehouseUseCase initiateReturnUseCase,
                                    ConfirmReturnedToWarehouseUseCase confirmReturnedUseCase,
                                    LookUpOrderForReturnUseCase lookUpOrderForReturnUseCase,
                                    GetPickpointCustomerReturnsUseCase getPickpointCustomerReturnsUseCase,
                                    ReceiveCustomerReturnUseCase receiveCustomerReturnUseCase,
                                    ConfirmCustomerReturnSentUseCase confirmCustomerReturnSentUseCase,
                                    LoadOrderPort loadOrderPort,
                                    LoadUserPort loadUserPort,
                                    @Value("${radolfa.delivery.pickpoint-storage-days:7}") int pickpointStorageDays) {
        this.getPickpointOrdersUseCase          = getPickpointOrdersUseCase;
        this.confirmWithDeliveryCodeUseCase     = confirmWithDeliveryCodeUseCase;
        this.confirmPickpointArrivalUseCase     = confirmPickpointArrivalUseCase;
        this.verifyPickupByCodeUseCase          = verifyPickupByCodeUseCase;
        this.initiateReturnUseCase              = initiateReturnUseCase;
        this.confirmReturnedUseCase             = confirmReturnedUseCase;
        this.lookUpOrderForReturnUseCase        = lookUpOrderForReturnUseCase;
        this.getPickpointCustomerReturnsUseCase = getPickpointCustomerReturnsUseCase;
        this.receiveCustomerReturnUseCase       = receiveCustomerReturnUseCase;
        this.confirmCustomerReturnSentUseCase   = confirmCustomerReturnSentUseCase;
        this.loadOrderPort                      = loadOrderPort;
        this.loadUserPort                       = loadUserPort;
        this.pickpointStorageDays               = pickpointStorageDays;
    }

    record ConfirmRequest(@NotBlank String code) {}

    record ReturnableItemDto(Long orderItemId, String productName, String skuCode,
                             int quantity, java.math.BigDecimal unitPrice) {}
    record ReturnableOrderDto(Long orderId, Long userId, List<ReturnableItemDto> items) {}

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

    @GetMapping("/orders/{orderId}/for-return")
    @PreAuthorize("hasRole('PICKPOINT_STAFF')")
    public ResponseEntity<ReturnableOrderDto> getOrderForReturn(
            @PathVariable Long orderId,
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {
        Order order = lookUpOrderForReturnUseCase.execute(orderId, principal.userId());
        List<ReturnableItemDto> items = order.items().stream()
                .map(i -> new ReturnableItemDto(
                        i.getId(), i.getProductName(), i.getSkuCode(),
                        i.getQuantity(), i.getPrice().amount()))
                .toList();
        return ResponseEntity.ok(new ReturnableOrderDto(order.id(), order.userId(), items));
    }

    @GetMapping("/customer-returns")
    @PreAuthorize("hasRole('PICKPOINT_STAFF')")
    public ResponseEntity<PageResponse<CustomerReturnDto>> getCustomerReturns(
            @RequestParam(defaultValue = "RECEIVED") CustomerReturnStatus status,
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {

        var result = getPickpointCustomerReturnsUseCase.execute(principal.userId(), status, page, size);

        var dtos = result.content().stream().map(r -> {
            Order order    = loadOrderPort.loadById(r.getOrderId()).orElseThrow();
            User  customer = loadUserPort.loadById(order.userId()).orElse(null);
            return CustomerReturnDto.from(r, order, customer);
        }).toList();

        return ResponseEntity.ok(PageResponse.from(
                new tj.radolfa.domain.model.PageResult<>(
                        dtos, result.totalElements(), result.number(), result.size(), result.last())));
    }

    @PostMapping("/customer-returns")
    @PreAuthorize("hasRole('PICKPOINT_STAFF')")
    public ResponseEntity<CustomerReturnDto> receiveCustomerReturn(
            @Valid @RequestBody CreateCustomerReturnRequestDto body,
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {

        var items = body.items().stream()
                .map(i -> new ReceiveCustomerReturnUseCase.ItemCommand(
                        i.orderItemId(), i.quantity(), i.reason(), i.notes()))
                .toList();
        var command = new ReceiveCustomerReturnUseCase.Command(
                body.orderId(), principal.userId(), body.notes(), items);

        var saved    = receiveCustomerReturnUseCase.execute(command);
        var order    = loadOrderPort.loadById(saved.getOrderId()).orElseThrow();
        var customer = loadUserPort.loadById(order.userId()).orElse(null);

        return ResponseEntity.status(201).body(CustomerReturnDto.from(saved, order, customer));
    }

    @PostMapping("/customer-returns/{returnId}/confirm-sent")
    @PreAuthorize("hasRole('PICKPOINT_STAFF')")
    public ResponseEntity<Void> confirmCustomerReturnSent(
            @PathVariable Long returnId,
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {
        confirmCustomerReturnSentUseCase.execute(returnId, principal.userId());
        return ResponseEntity.noContent().build();
    }

}
