package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.loyalty.RestoreLoyaltyPointsUseCase;
import tj.radolfa.application.ports.out.LoadCartPort;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.application.ports.out.SaveCartPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.application.ports.out.StockAdjustmentPort;
import tj.radolfa.domain.model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ConfirmRecallReceivedServiceTest {

    static final Long COURIER_ID   = 55L;
    static final Long PICKPOINT_ID = 5L;
    static final Long ORDER_ID     = 1L;
    static final Long SKU_A        = 101L;
    static final Long SKU_B        = 102L;

    // ── Fakes ────────────────────────────────────────────────────────────────

    static Order recalledOrderWithCourier() {
        return new Order.Builder()
                .id(ORDER_ID).userId(10L).status(OrderStatus.RECALL_REQUESTED)
                .courierId(COURIER_ID)
                .totalAmount(new Money(BigDecimal.valueOf(300))).createdAt(Instant.now())
                .deliveryType(DeliveryType.HOME)
                .build();
    }

    static Order recalledOrderAtPickpoint() {
        return new Order.Builder()
                .id(ORDER_ID).userId(10L).status(OrderStatus.RECALL_REQUESTED)
                .pickpointId(PICKPOINT_ID)
                .totalAmount(new Money(BigDecimal.valueOf(300))).createdAt(Instant.now())
                .deliveryType(DeliveryType.PICKPOINT)
                .build();
    }

    static Order recalledOrderWithItems(int loyaltyRedeemed) {
        var item1 = new OrderItem(1L, SKU_A, null, "SKU-A", "Widget", 2, new Money(BigDecimal.valueOf(100)));
        var item2 = new OrderItem(2L, SKU_B, null, "SKU-B", "Gadget", 1, new Money(BigDecimal.valueOf(50)));
        return new Order.Builder()
                .id(ORDER_ID).userId(10L).status(OrderStatus.RECALL_REQUESTED)
                .courierId(COURIER_ID)
                .items(List.of(item1, item2))
                .loyaltyPointsRedeemed(loyaltyRedeemed)
                .totalAmount(new Money(BigDecimal.valueOf(250))).createdAt(Instant.now())
                .deliveryType(DeliveryType.HOME)
                .build();
    }

    static User courierUser() {
        return new User(COURIER_ID, new PhoneNumber("992000000055"), UserRole.COURIER,
                "Courier", null, null, true, 1L);
    }

    static User wrongCourierUser() {
        return new User(66L, new PhoneNumber("992000000066"), UserRole.COURIER,
                "Wrong Courier", null, null, true, 1L);
    }

    static User pickpointStaff() {
        return new User(77L, new PhoneNumber("992000000077"), UserRole.PICKPOINT_STAFF,
                "Staff", null, null, true, 1L, null, null, null, null, null, PICKPOINT_ID, null);
    }

    static User adminUser() {
        return new User(99L, new PhoneNumber("992000000099"), UserRole.ADMIN,
                "Admin", null, LoyaltyProfile.empty(), true, 1L);
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

    static LoadUserPort userPort(User user) {
        return new LoadUserPort() {
            @Override public Optional<User> loadById(Long id) {
                return user != null && user.id().equals(id) ? Optional.of(user) : Optional.empty();
            }
            @Override public Optional<User> loadByPhone(String p) { return Optional.empty(); }
            @Override public List<User> findAllNonPermanent() { return List.of(); }
            @Override public List<User> findByRoleAndEnabledTrue(UserRole r) { return List.of(); }
        };
    }

    static class CapturingSaveOrderPort implements SaveOrderPort {
        final List<Order> saved = new ArrayList<>();
        @Override public Order save(Order o) { saved.add(o); return o; }
        Order last() { return saved.get(saved.size() - 1); }
    }

    static class CapturingStockAdjustmentPort implements StockAdjustmentPort {
        record Increment(Long skuId, int qty) {}
        final List<Increment> increments = new ArrayList<>();
        @Override public void increment(Long skuId, int qty) { increments.add(new Increment(skuId, qty)); }
        @Override public void decrement(Long skuId, int qty) {}
        @Override public void setAbsolute(Long skuId, int qty) {}
    }

    static class CapturingRestoreLoyaltyPort implements RestoreLoyaltyPointsUseCase {
        int restoredPoints = 0;
        boolean called = false;
        @Override public void execute(Long userId, int points) { called = true; restoredPoints = points; }
    }

    static NotificationPort silentNotificationPort() {
        return new NotificationPort() {
            @Override public void sendOrderConfirmation(Long u, Long o) {}
            @Override public void sendOrderStatusUpdate(Long u, Long o, OrderStatus s) {}
            @Override public void sendReviewApprovedNotification(Long u, Long r) {}
            @Override public void sendReviewReplyNotification(Long u, Long r) {}
            @Override public void sendDeliveryCode(Long u, Long o, String c, Instant e) {}
            @Override public void sendPickpointExpiryWarning(Long u, Long o, int d) {}
            @Override public void sendPickpointOrderExpiredCancellation(Long u, Long o) {}
        };
    }

    static ConfirmRecallReceivedService service(Order order, User actor,
                                                 CapturingSaveOrderPort save,
                                                 CapturingStockAdjustmentPort stock,
                                                 CapturingRestoreLoyaltyPort loyalty) {
        return new ConfirmRecallReceivedService(
                orderPort(order),
                save,
                userPort(actor),
                stock,
                loyalty,
                new LoadCartPort() {
                    @Override public Optional<tj.radolfa.domain.model.Cart> findActiveByUserId(Long id) { return Optional.empty(); }
                    @Override public Optional<tj.radolfa.domain.model.Cart> findById(Long id) { return Optional.empty(); }
                },
                (cart) -> cart,
                new OrderNotificationService(silentNotificationPort()));
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("RECALL_REQUESTED confirmed by assigned courier → CANCELLED, recall confirmed fields set")
    void confirmedByCourier_orderCancelled() {
        var save    = new CapturingSaveOrderPort();
        var stock   = new CapturingStockAdjustmentPort();
        var loyalty = new CapturingRestoreLoyaltyPort();
        service(recalledOrderWithCourier(), courierUser(), save, stock, loyalty)
                .execute(ORDER_ID, COURIER_ID);

        assertEquals(OrderStatus.CANCELLED, save.last().status());
        assertNotNull(save.last().cancelledAt());
        assertNotNull(save.last().recallConfirmedAt());
        assertEquals(COURIER_ID, save.last().recallConfirmedByUserId());
    }

    @Test
    @DisplayName("RECALL_REQUESTED at pickpoint confirmed by correct pickpoint staff → CANCELLED")
    void confirmedByPickpointStaff_orderCancelled() {
        var staff = pickpointStaff();
        var save  = new CapturingSaveOrderPort();
        service(recalledOrderAtPickpoint(), staff, save,
                new CapturingStockAdjustmentPort(), new CapturingRestoreLoyaltyPort())
                .execute(ORDER_ID, staff.id());

        assertEquals(OrderStatus.CANCELLED, save.last().status());
    }

    @Test
    @DisplayName("ADMIN confirms any RECALL_REQUESTED order → CANCELLED (no ownership check)")
    void adminConfirms_orderCancelled() {
        var save = new CapturingSaveOrderPort();
        service(recalledOrderWithCourier(), adminUser(), save,
                new CapturingStockAdjustmentPort(), new CapturingRestoreLoyaltyPort())
                .execute(ORDER_ID, 99L);

        assertEquals(OrderStatus.CANCELLED, save.last().status());
    }

    @Test
    @DisplayName("Wrong status (SHIPPED) → IllegalStateException")
    void wrongStatus_throwsIllegalState() {
        Order shipped = new Order.Builder()
                .id(ORDER_ID).userId(10L).status(OrderStatus.SHIPPED)
                .courierId(COURIER_ID)
                .totalAmount(new Money(BigDecimal.valueOf(300))).createdAt(Instant.now())
                .deliveryType(DeliveryType.HOME).build();

        assertThrows(IllegalStateException.class,
                () -> service(shipped, courierUser(), new CapturingSaveOrderPort(),
                        new CapturingStockAdjustmentPort(), new CapturingRestoreLoyaltyPort())
                        .execute(ORDER_ID, COURIER_ID));
    }

    @Test
    @DisplayName("Courier from different order tries to confirm → IllegalStateException")
    void wrongCourier_throwsIllegalState() {
        assertThrows(IllegalStateException.class,
                () -> service(recalledOrderWithCourier(), wrongCourierUser(),
                        new CapturingSaveOrderPort(), new CapturingStockAdjustmentPort(),
                        new CapturingRestoreLoyaltyPort())
                        .execute(ORDER_ID, wrongCourierUser().id()));
    }

    @Test
    @DisplayName("Order with 2 items with different SKUs → both incremented")
    void twoItems_bothStockRestored() {
        var stock   = new CapturingStockAdjustmentPort();
        var loyalty = new CapturingRestoreLoyaltyPort();
        service(recalledOrderWithItems(0), courierUser(), new CapturingSaveOrderPort(), stock, loyalty)
                .execute(ORDER_ID, COURIER_ID);

        assertEquals(2, stock.increments.size());
        assertTrue(stock.increments.stream().anyMatch(i -> i.skuId().equals(SKU_A) && i.qty() == 2));
        assertTrue(stock.increments.stream().anyMatch(i -> i.skuId().equals(SKU_B) && i.qty() == 1));
    }

    @Test
    @DisplayName("Order with no loyalty redemption → restoreLoyaltyPointsUseCase not called")
    void noLoyaltyRedemption_loyaltyNotRestored() {
        var loyalty = new CapturingRestoreLoyaltyPort();
        service(recalledOrderWithItems(0), courierUser(), new CapturingSaveOrderPort(),
                new CapturingStockAdjustmentPort(), loyalty)
                .execute(ORDER_ID, COURIER_ID);

        assertFalse(loyalty.called);
    }

    @Test
    @DisplayName("Order with loyalty redeemed > 0 → points restored")
    void withLoyaltyRedemption_pointsRestored() {
        var loyalty = new CapturingRestoreLoyaltyPort();
        service(recalledOrderWithItems(100), courierUser(), new CapturingSaveOrderPort(),
                new CapturingStockAdjustmentPort(), loyalty)
                .execute(ORDER_ID, COURIER_ID);

        assertTrue(loyalty.called);
        assertEquals(100, loyalty.restoredPoints);
    }
}
