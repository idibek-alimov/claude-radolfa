package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.order.UpdateOrderStatusUseCase.Command;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UpdateOrderStatusServiceTest {

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static Order homeOrder(OrderStatus status) {
        return new Order(1L, 10L, null, status,
                new Money(BigDecimal.valueOf(500)), List.of(), Instant.now(),
                0, 0,
                DeliveryType.HOME, "123 Main St", "MORNING", null,
                null, null, null);
    }

    static Order homeShippedOrder() {
        return new Order(1L, 10L, null, OrderStatus.SHIPPED,
                new Money(BigDecimal.valueOf(500)), List.of(), Instant.now(),
                0, 0,
                DeliveryType.HOME, "123 Main St", "MORNING", null,
                "DHL", "TST123", LocalDate.of(2026, 6, 1));
    }

    static Order pickpointOrder(OrderStatus status) {
        return new Order(2L, 10L, null, status,
                new Money(BigDecimal.valueOf(300)), List.of(), Instant.now(),
                0, 0,
                DeliveryType.PICKPOINT, null, null, 99L,
                null, null, null);
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

    static class CapturingSaveOrderPort implements SaveOrderPort {
        final List<Order> saved = new ArrayList<>();
        @Override public Order save(Order order) { saved.add(order); return order; }
        Order last() { return saved.get(saved.size() - 1); }
    }

    static UpdateOrderStatusService service(Order order, CapturingSaveOrderPort save) {
        return new UpdateOrderStatusService(orderPort(order), save);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("HOME PAID→SHIPPED with courierName succeeds; courier fields persisted")
    void homeShipWithCourier_succeeds() {
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();
        UpdateOrderStatusService svc = service(homeOrder(OrderStatus.PAID), save);

        svc.execute(new Command(1L, OrderStatus.SHIPPED, "DHL", "TST123",
                LocalDate.of(2026, 6, 1)));

        Order saved = save.last();
        assertEquals(OrderStatus.SHIPPED, saved.status());
        assertEquals("DHL", saved.courierName());
        assertEquals("TST123", saved.trackingNumber());
        assertEquals(LocalDate.of(2026, 6, 1), saved.estimatedDeliveryDate());
    }

    @Test
    @DisplayName("HOME PAID→SHIPPED without courierName throws IllegalArgumentException")
    void homeShipWithoutCourier_throws() {
        UpdateOrderStatusService svc = service(homeOrder(OrderStatus.PAID), new CapturingSaveOrderPort());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> svc.execute(new Command(1L, OrderStatus.SHIPPED, null, null, null)));
        assertTrue(ex.getMessage().contains("Courier name is required"));
    }

    @Test
    @DisplayName("HOME PAID→SHIPPED with blank courierName throws")
    void homeShipWithBlankCourier_throws() {
        UpdateOrderStatusService svc = service(homeOrder(OrderStatus.PAID), new CapturingSaveOrderPort());

        assertThrows(IllegalArgumentException.class,
                () -> svc.execute(new Command(1L, OrderStatus.SHIPPED, "  ", null, null)));
    }

    @Test
    @DisplayName("PICKPOINT PAID→SHIPPED without courierName succeeds")
    void pickpointShipWithoutCourier_succeeds() {
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();
        UpdateOrderStatusService svc = service(pickpointOrder(OrderStatus.PAID), save);

        svc.execute(new Command(2L, OrderStatus.SHIPPED, null, null, null));

        assertEquals(OrderStatus.SHIPPED, save.last().status());
        assertNull(save.last().courierName());
    }

    @Test
    @DisplayName("PENDING→PAID with stray courierName: succeeds, courier fields NOT written")
    void pendingToPaid_courierFieldsIgnored() {
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();
        UpdateOrderStatusService svc = service(homeOrder(OrderStatus.PENDING), save);

        svc.execute(new Command(1L, OrderStatus.PAID, "DHL", "X", LocalDate.now()));

        Order saved = save.last();
        assertEquals(OrderStatus.PAID, saved.status());
        assertNull(saved.courierName());
        assertNull(saved.trackingNumber());
        assertNull(saved.estimatedDeliveryDate());
    }

    @Test
    @DisplayName("SHIPPED→DELIVERED preserves existing courier fields")
    void shippedToDelivered_courierFieldsCarriedThrough() {
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();
        UpdateOrderStatusService svc = service(homeShippedOrder(), save);

        svc.execute(new Command(1L, OrderStatus.DELIVERED, null, null, null));

        Order saved = save.last();
        assertEquals(OrderStatus.DELIVERED, saved.status());
        assertEquals("DHL", saved.courierName());
        assertEquals("TST123", saved.trackingNumber());
        assertEquals(LocalDate.of(2026, 6, 1), saved.estimatedDeliveryDate());
    }

    @Test
    @DisplayName("Invalid transition PAID→PENDING throws")
    void invalidTransition_throws() {
        UpdateOrderStatusService svc = service(homeOrder(OrderStatus.PAID), new CapturingSaveOrderPort());

        assertThrows(IllegalArgumentException.class,
                () -> svc.execute(new Command(1L, OrderStatus.PENDING, null, null, null)));
    }
}
