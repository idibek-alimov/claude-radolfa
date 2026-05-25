package tj.radolfa.application.services;

import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.order.GetDeliveryCodeUseCase;
import tj.radolfa.application.ports.out.LoadDeliveryCodePort;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.domain.exception.CourierAccessDeniedException;
import tj.radolfa.domain.exception.DeliveryCodeNotFoundException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.DeliveryCode;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GetDeliveryCodeServiceTest {

    private static final Long ORDER_ID = 1L;
    private static final Long USER_ID  = 10L;
    private static final Long OTHER_ID = 99L;

    static Order order(OrderStatus status) {
        return new Order.Builder()
                .id(ORDER_ID).userId(USER_ID).status(status)
                .totalAmount(new Money(BigDecimal.TEN)).createdAt(Instant.now())
                .deliveryType(DeliveryType.HOME).deliveryAddress("Addr")
                .build();
    }

    static DeliveryCode activeCode() {
        return new DeliveryCode(1L, ORDER_ID, "123456",
                Instant.now().plusSeconds(3600), null, 0, Instant.now());
    }

    static LoadOrderPort orderPort(Order order) {
        return new LoadOrderPort() {
            @Override public Optional<Order> loadById(Long id) {
                return order != null && order.id().equals(id) ? Optional.of(order) : Optional.empty();
            }
            @Override public List<Order> loadByUserId(Long u) { return List.of(); }
            @Override public Optional<Order> loadByExternalOrderId(String e) { return Optional.empty(); }
            @Override public List<Order> loadRecentPaidByUserId(Long u, int l) { return List.of(); }
        };
    }

    static LoadDeliveryCodePort codePort(Optional<DeliveryCode> code) {
        return orderId -> code;
    }

    static GetDeliveryCodeService service(Order order, Optional<DeliveryCode> code) {
        return new GetDeliveryCodeService(orderPort(order), codePort(code));
    }

    @Test
    void shippedOrder_returnsCode() {
        var svc = service(order(OrderStatus.SHIPPED), Optional.of(activeCode()));
        GetDeliveryCodeUseCase.Result result = svc.execute(ORDER_ID, USER_ID);
        assertEquals("123456", result.code());
        assertNotNull(result.expiresAt());
    }

    @Test
    void readyForPickup_returnsCode() {
        var svc = service(order(OrderStatus.READY_FOR_PICKUP), Optional.of(activeCode()));
        assertEquals("123456", svc.execute(ORDER_ID, USER_ID).code());
    }

    @Test
    void outForDelivery_returnsCode() {
        var svc = service(order(OrderStatus.OUT_FOR_DELIVERY), Optional.of(activeCode()));
        assertEquals("123456", svc.execute(ORDER_ID, USER_ID).code());
    }

    @Test
    void orderNotFound_throwsResourceNotFoundException() {
        var svc = service(null, Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> svc.execute(ORDER_ID, USER_ID));
    }

    @Test
    void wrongUser_throwsCourierAccessDeniedException() {
        var svc = service(order(OrderStatus.SHIPPED), Optional.of(activeCode()));
        assertThrows(CourierAccessDeniedException.class, () -> svc.execute(ORDER_ID, OTHER_ID));
    }

    @Test
    void wrongStatus_delivered_throwsIllegalArgumentException() {
        var svc = service(order(OrderStatus.DELIVERED), Optional.of(activeCode()));
        assertThrows(IllegalArgumentException.class, () -> svc.execute(ORDER_ID, USER_ID));
    }

    @Test
    void noActiveCode_throwsDeliveryCodeNotFoundException() {
        var svc = service(order(OrderStatus.SHIPPED), Optional.empty());
        assertThrows(DeliveryCodeNotFoundException.class, () -> svc.execute(ORDER_ID, USER_ID));
    }
}
