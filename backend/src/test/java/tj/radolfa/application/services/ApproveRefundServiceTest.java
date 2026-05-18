package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadCustomerReturnPort;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadPaymentPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.application.ports.out.ProcessRefundPort;
import tj.radolfa.application.ports.out.SaveCustomerReturnPort;
import tj.radolfa.domain.exception.RefundFailedException;
import tj.radolfa.domain.model.CustomerReturn;
import tj.radolfa.domain.model.CustomerReturnItem;
import tj.radolfa.domain.model.CustomerReturnStatus;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.Payment;
import tj.radolfa.domain.model.PaymentStatus;
import tj.radolfa.domain.model.ReturnReason;
import tj.radolfa.domain.model.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ApproveRefundServiceTest {

    static final Long ORDER_ID     = 10L;
    static final Long RETURN_ID    = 1L;
    static final Long ADMIN_ID     = 99L;
    static final Long ORDER_ITEM_1 = 101L;
    static final Long ORDER_ITEM_2 = 102L;

    // ── Domain object factories ───────────────────────────────────────────────

    static OrderItem item(Long id, BigDecimal unitPrice, int qty) {
        return new OrderItem(id, null, null, null, "Product", qty, Money.of(unitPrice));
    }

    static Order orderWith(List<OrderItem> items) {
        return new Order.Builder()
                .id(ORDER_ID).userId(5L).status(OrderStatus.DELIVERED)
                .items(items)
                .deliveryType(DeliveryType.PICKPOINT)
                .totalAmount(Money.of(new BigDecimal("100.00")))
                .createdAt(Instant.now())
                .build();
    }

    static CustomerReturn returnWith(CustomerReturnStatus status, List<CustomerReturnItem> items) {
        return new CustomerReturn(RETURN_ID, ORDER_ID, 5L, 7L, Instant.now(), null, items,
                status, null, null, null, null, null, null);
    }

    static List<CustomerReturnItem> singleItem(Long orderItemId) {
        return List.of(new CustomerReturnItem(1L, RETURN_ID, orderItemId, 1, ReturnReason.DAMAGED, null));
    }

    static Payment payment() {
        return new Payment(1L, ORDER_ID, Money.of(new BigDecimal("100.00")), "TJS",
                PaymentStatus.COMPLETED, "payme", "TX-001", null, Instant.now(), Instant.now());
    }

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static LoadCustomerReturnPort returnPort(CustomerReturn r) {
        return new LoadCustomerReturnPort() {
            @Override public Optional<CustomerReturn> loadById(Long id) {
                return r != null && r.getId().equals(id) ? Optional.of(r) : Optional.empty();
            }
            @Override public List<CustomerReturn> loadAllByOrderId(Long orderId) { return List.of(); }
            @Override public PageResult<CustomerReturn> loadByPickpointIdAndStatus(
                    Long p, CustomerReturnStatus s, int pg, int sz) {
                return new PageResult<>(List.of(), 0, 1, sz, true);
            }
            @Override public PageResult<CustomerReturn> loadAllPaged(int pg, int sz, String search) {
                return new PageResult<>(List.of(), 0, 1, sz, true);
            }
        };
    }

    static class CapturingSavePort implements SaveCustomerReturnPort {
        // Snapshot status at each save — CustomerReturn is mutable so the same reference
        // would show the final state if we stored it directly.
        final List<CustomerReturnStatus> savedStatuses = new ArrayList<>();
        String lastGatewayRefundId;
        @Override public CustomerReturn save(CustomerReturn r) {
            savedStatuses.add(r.getStatus());
            if (r.getGatewayRefundId() != null) lastGatewayRefundId = r.getGatewayRefundId();
            return r;
        }
        boolean isEmpty() { return savedStatuses.isEmpty(); }
    }

    static LoadOrderPort orderPort(Order o) {
        return new LoadOrderPort() {
            @Override public List<Order> loadByUserId(Long userId) { return List.of(); }
            @Override public Optional<Order> loadById(Long id) {
                return id.equals(ORDER_ID) ? Optional.of(o) : Optional.empty();
            }
            @Override public Optional<Order> loadByExternalOrderId(String extId) { return Optional.empty(); }
            @Override public List<Order> loadRecentPaidByUserId(Long userId, int limit) { return List.of(); }
        };
    }

    static LoadUserPort userPort() {
        return new LoadUserPort() {
            @Override public Optional<User> loadByPhone(String phone) { return Optional.empty(); }
            @Override public Optional<User> loadById(Long id) { return Optional.empty(); }
            @Override public List<User> findAllNonPermanent() { return List.of(); }
            @Override public List<User> findByRoleAndEnabledTrue(tj.radolfa.domain.model.UserRole role) { return List.of(); }
        };
    }

    static LoadPaymentPort paymentPort(Payment p) {
        return new LoadPaymentPort() {
            @Override public Optional<Payment> findByOrderId(Long id) { return Optional.ofNullable(p); }
            @Override public Optional<Payment> findByProviderTransactionId(String txId) { return Optional.empty(); }
        };
    }

    static class ConfigurableProcessRefundPort implements ProcessRefundPort {
        final boolean success;
        final String  failureReason;
        Money capturedAmount;
        ConfigurableProcessRefundPort(boolean success, String failureReason) {
            this.success = success; this.failureReason = failureReason;
        }
        @Override public RefundResult process(Long orderId, String externalPaymentId, Money amount) {
            this.capturedAmount = amount;
            return new RefundResult(success, success ? "STUB-123" : null, failureReason);
        }
    }

    static class CapturingNotificationPort implements NotificationPort {
        Long capturedUserId; Long capturedOrderId; Money capturedAmount;
        @Override public void sendOrderConfirmation(Long u, Long o) {}
        @Override public void sendOrderStatusUpdate(Long u, Long o, OrderStatus s) {}
        @Override public void sendReviewApprovedNotification(Long u, Long r) {}
        @Override public void sendReviewReplyNotification(Long u, Long r) {}
        @Override public void sendDeliveryCode(Long u, Long o, String c, Instant e) {}
        @Override public void sendPickpointExpiryWarning(Long u, Long o, int d) {}
        @Override public void sendPickpointOrderExpiredCancellation(Long u, Long o) {}
        @Override public void sendRefundApprovedNotification(Long userId, Long orderId, Money amount) {
            capturedUserId = userId; capturedOrderId = orderId; capturedAmount = amount;
        }
    }

    ApproveRefundService service(CustomerReturn customerReturn,
                                  Order order,
                                  ConfigurableProcessRefundPort processPort,
                                  CapturingSavePort savePort,
                                  CapturingNotificationPort notifPort) {
        return new ApproveRefundService(
                returnPort(customerReturn),
                savePort,
                orderPort(order),
                userPort(),
                paymentPort(payment()),
                processPort,
                notifPort);
    }

    // ── Tests ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Happy path: SENT_TO_WAREHOUSE → REFUNDED, amount correct, SMS sent")
    void happyPath() {
        var orderItems = List.of(item(ORDER_ITEM_1, new BigDecimal("50.00"), 1));
        var customerReturn = returnWith(CustomerReturnStatus.SENT_TO_WAREHOUSE, singleItem(ORDER_ITEM_1));
        var savePort   = new CapturingSavePort();
        var processPort = new ConfigurableProcessRefundPort(true, null);
        var notifPort  = new CapturingNotificationPort();

        service(customerReturn, orderWith(orderItems), processPort, savePort, notifPort)
                .execute(RETURN_ID, ADMIN_ID);

        assertEquals(new BigDecimal("50.00"), processPort.capturedAmount.amount());
        assertEquals(2, savePort.savedStatuses.size());
        assertEquals(CustomerReturnStatus.REFUND_APPROVED, savePort.savedStatuses.get(0));
        assertEquals(CustomerReturnStatus.REFUNDED,        savePort.savedStatuses.get(1));
        assertEquals("STUB-123", savePort.lastGatewayRefundId);
        assertEquals(5L,       notifPort.capturedUserId);
        assertEquals(ORDER_ID, notifPort.capturedOrderId);
        assertEquals(new BigDecimal("50.00"), notifPort.capturedAmount.amount());
    }

    @Test
    @DisplayName("RECEIVED state → IllegalStateException; nothing saved, no refund call")
    void wrongStateReceived() {
        var orderItems = List.of(item(ORDER_ITEM_1, new BigDecimal("50.00"), 1));
        var customerReturn = returnWith(CustomerReturnStatus.RECEIVED, singleItem(ORDER_ITEM_1));
        var savePort   = new CapturingSavePort();
        var processPort = new ConfigurableProcessRefundPort(true, null);

        assertThrows(IllegalStateException.class,
                () -> service(customerReturn, orderWith(orderItems),
                        processPort, savePort, new CapturingNotificationPort()).execute(RETURN_ID, ADMIN_ID));

        assertTrue(savePort.isEmpty());
        assertNull(processPort.capturedAmount);
    }

    @Test
    @DisplayName("Already REFUND_APPROVED → IllegalStateException (double-refund guard)")
    void alreadyRefundApproved() {
        var orderItems = List.of(item(ORDER_ITEM_1, new BigDecimal("50.00"), 1));
        var customerReturn = returnWith(CustomerReturnStatus.REFUND_APPROVED, singleItem(ORDER_ITEM_1));
        var processPort = new ConfigurableProcessRefundPort(true, null);

        assertThrows(IllegalStateException.class,
                () -> service(customerReturn, orderWith(orderItems),
                        processPort, new CapturingSavePort(), new CapturingNotificationPort())
                        .execute(RETURN_ID, ADMIN_ID));

        assertNull(processPort.capturedAmount);
    }

    @Test
    @DisplayName("Gateway failure → RefundFailedException; return status unchanged")
    void gatewayFailure() {
        var orderItems = List.of(item(ORDER_ITEM_1, new BigDecimal("50.00"), 1));
        var customerReturn = returnWith(CustomerReturnStatus.SENT_TO_WAREHOUSE, singleItem(ORDER_ITEM_1));
        var savePort   = new CapturingSavePort();
        var processPort = new ConfigurableProcessRefundPort(false, "Insufficient funds");

        assertThrows(RefundFailedException.class,
                () -> service(customerReturn, orderWith(orderItems),
                        processPort, savePort, new CapturingNotificationPort()).execute(RETURN_ID, ADMIN_ID));

        assertTrue(savePort.isEmpty());
    }

    @Test
    @DisplayName("Refund amount re-derived from order items — 2 items, different prices")
    void amountRederived() {
        // order: item1 qty=1 @ 50.00, item2 qty=2 @ 30.00
        var orderItems = List.of(
                item(ORDER_ITEM_1, new BigDecimal("50.00"), 1),
                item(ORDER_ITEM_2, new BigDecimal("30.00"), 2));
        // return: item1 qty=1, item2 qty=1 → 50.00 + 30.00 = 80.00
        var returnItems = List.of(
                new CustomerReturnItem(1L, RETURN_ID, ORDER_ITEM_1, 1, ReturnReason.DAMAGED, null),
                new CustomerReturnItem(2L, RETURN_ID, ORDER_ITEM_2, 1, ReturnReason.WRONG_ITEM, null));
        var customerReturn = returnWith(CustomerReturnStatus.SENT_TO_WAREHOUSE, returnItems);
        var processPort = new ConfigurableProcessRefundPort(true, null);

        service(customerReturn, orderWith(orderItems), processPort,
                new CapturingSavePort(), new CapturingNotificationPort())
                .execute(RETURN_ID, ADMIN_ID);

        assertEquals(new BigDecimal("80.00"), processPort.capturedAmount.amount());
    }
}
