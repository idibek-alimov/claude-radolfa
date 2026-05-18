package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.order.ReceiveCustomerReturnUseCase;
import tj.radolfa.application.ports.out.LoadCustomerReturnPort;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.application.ports.out.SaveCustomerReturnPort;
import tj.radolfa.domain.exception.OrderNotAtPickpointException;
import tj.radolfa.domain.exception.OrderNotDeliveredException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.exception.ReturnAlreadyExistsException;
import tj.radolfa.domain.exception.ReturnItemQuantityExceededException;
import tj.radolfa.domain.model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ReceiveCustomerReturnServiceTest {

    static final Long PICKPOINT_ID = 5L;
    static final Long ORDER_ID     = 1L;
    static final Long STAFF_ID     = 99L;
    static final Long ITEM_A_ID    = 10L;
    static final Long ITEM_B_ID    = 11L;

    // ── Domain factories ─────────────────────────────────────────────────────

    static Order deliveredPickpointOrder(List<OrderItem> items) {
        return new Order.Builder()
                .id(ORDER_ID).userId(20L)
                .status(OrderStatus.DELIVERED)
                .deliveryType(DeliveryType.PICKPOINT)
                .pickpointId(PICKPOINT_ID)
                .totalAmount(new Money(BigDecimal.valueOf(300)))
                .createdAt(Instant.now())
                .items(items)
                .build();
    }

    static OrderItem itemA(int qty) {
        return new OrderItem(ITEM_A_ID, null, null, "SKU-A", "Product A", qty,
                new Money(BigDecimal.valueOf(100)));
    }

    static OrderItem itemB(int qty) {
        return new OrderItem(ITEM_B_ID, null, null, "SKU-B", "Product B", qty,
                new Money(BigDecimal.valueOf(50)));
    }

    static User staffUser() {
        return new User(STAFF_ID, new PhoneNumber("+992000000001"), UserRole.PICKPOINT_STAFF,
                "Staff", null, null, true, 1L,
                null, null, null, null, null, PICKPOINT_ID, null);
    }

    static CustomerReturn receivedReturn(Long itemId, int qty) {
        return new CustomerReturn(1L, ORDER_ID, PICKPOINT_ID, STAFF_ID, Instant.now(),
                null,
                List.of(new CustomerReturnItem(null, 1L, itemId, qty, ReturnReason.DAMAGED, null)),
                CustomerReturnStatus.RECEIVED,
                null, null, null, null, null, null);
    }

    static CustomerReturn sentReturn(Long itemId, int qty) {
        var r = new CustomerReturn(2L, ORDER_ID, PICKPOINT_ID, STAFF_ID, Instant.now(),
                null,
                List.of(new CustomerReturnItem(null, 2L, itemId, qty, ReturnReason.DAMAGED, null)),
                CustomerReturnStatus.RECEIVED,
                null, null, null, null, null, null);
        r.markSentToWarehouse(STAFF_ID);
        return r;
    }

    // ── Fake ports ───────────────────────────────────────────────────────────

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

    static LoadUserPort userPort(User u) {
        return new LoadUserPort() {
            @Override public Optional<User> loadById(Long id) {
                return u != null && u.id().equals(id) ? Optional.of(u) : Optional.empty();
            }
            @Override public Optional<User> loadByPhone(String p) { return Optional.empty(); }
            @Override public List<User> findAllNonPermanent() { return List.of(); }
            @Override public List<User> findByRoleAndEnabledTrue(UserRole r) { return List.of(); }
        };
    }

    static LoadCustomerReturnPort returnPort(List<CustomerReturn> existing) {
        return new LoadCustomerReturnPort() {
            @Override public Optional<CustomerReturn> loadById(Long id) { return Optional.empty(); }
            @Override public List<CustomerReturn> loadAllByOrderId(Long orderId) { return existing; }
            @Override public PageResult<CustomerReturn> loadByPickpointIdAndStatus(Long p, CustomerReturnStatus s, int pg, int sz) {
                return new PageResult<>(List.of(), 0, 1, sz, true);
            }
            @Override public PageResult<CustomerReturn> loadAllPaged(int pg, int sz, String search) {
                return new PageResult<>(List.of(), 0, 1, sz, true);
            }
        };
    }

    static class CapturingSavePort implements SaveCustomerReturnPort {
        final List<CustomerReturn> saved = new ArrayList<>();
        @Override public CustomerReturn save(CustomerReturn r) { saved.add(r); return r; }
        CustomerReturn last() { return saved.get(saved.size() - 1); }
    }

    static class CapturingNotificationPort implements NotificationPort {
        final List<Long[]> returnReceivedCalls = new ArrayList<>();
        @Override public void sendCustomerReturnReceivedNotification(Long uid, Long oid) {
            returnReceivedCalls.add(new Long[]{uid, oid});
        }
        @Override public void sendOrderConfirmation(Long u, Long o) {}
        @Override public void sendOrderStatusUpdate(Long u, Long o, OrderStatus s) {}
        @Override public void sendReviewApprovedNotification(Long u, Long r) {}
        @Override public void sendReviewReplyNotification(Long u, Long r) {}
        @Override public void sendDeliveryCode(Long u, Long o, String c, java.time.Instant e) {}
        @Override public void sendPickpointExpiryWarning(Long u, Long o, int d) {}
        @Override public void sendPickpointOrderExpiredCancellation(Long u, Long o) {}
    }

    static ReceiveCustomerReturnService service(Order order, User staff,
                                                  List<CustomerReturn> existing,
                                                  CapturingSavePort save) {
        return new ReceiveCustomerReturnService(
                orderPort(order), userPort(staff), returnPort(existing), save,
                new CapturingNotificationPort());
    }

    static ReceiveCustomerReturnService service(Order order, User staff,
                                                  List<CustomerReturn> existing,
                                                  CapturingSavePort save,
                                                  CapturingNotificationPort notif) {
        return new ReceiveCustomerReturnService(
                orderPort(order), userPort(staff), returnPort(existing), save, notif);
    }

    static ReceiveCustomerReturnUseCase.Command command(Long itemId, int qty) {
        return new ReceiveCustomerReturnUseCase.Command(ORDER_ID, STAFF_ID, null,
                List.of(new ReceiveCustomerReturnUseCase.ItemCommand(itemId, qty, ReturnReason.DAMAGED, null)));
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELIVERED PICKPOINT order at correct pickpoint → CustomerReturn saved with RECEIVED status")
    void validReturn_savesWithReceivedStatus() {
        var save  = new CapturingSavePort();
        var order = deliveredPickpointOrder(List.of(itemA(1)));
        service(order, staffUser(), List.of(), save).execute(command(ITEM_A_ID, 1));

        assertEquals(CustomerReturnStatus.RECEIVED, save.last().getStatus());
        assertEquals(STAFF_ID, save.last().getReceivedByStaffId());
        assertNotNull(save.last().getReceivedAt());
        assertEquals(1, save.last().getItems().size());
    }

    @Test
    @DisplayName("Order not DELIVERED → OrderNotDeliveredException")
    void orderNotDelivered_throws() {
        var paidOrder = new Order.Builder()
                .id(ORDER_ID).userId(20L).status(OrderStatus.PAID)
                .deliveryType(DeliveryType.PICKPOINT).pickpointId(PICKPOINT_ID)
                .totalAmount(new Money(BigDecimal.valueOf(300))).createdAt(Instant.now())
                .items(List.of(itemA(1))).build();

        assertThrows(OrderNotDeliveredException.class,
                () -> service(paidOrder, staffUser(), List.of(), new CapturingSavePort())
                        .execute(command(ITEM_A_ID, 1)));
    }

    @Test
    @DisplayName("Order at a different pickpoint → OrderNotAtPickpointException")
    void differentPickpoint_throws() {
        var wrongPickpointOrder = new Order.Builder()
                .id(ORDER_ID).userId(20L).status(OrderStatus.DELIVERED)
                .deliveryType(DeliveryType.PICKPOINT).pickpointId(99L)
                .totalAmount(new Money(BigDecimal.valueOf(300))).createdAt(Instant.now())
                .items(List.of(itemA(1))).build();

        assertThrows(OrderNotAtPickpointException.class,
                () -> service(wrongPickpointOrder, staffUser(), List.of(), new CapturingSavePort())
                        .execute(command(ITEM_A_ID, 1)));
    }

    @Test
    @DisplayName("Open RECEIVED return already exists → ReturnAlreadyExistsException")
    void openReturnExists_throws() {
        var order = deliveredPickpointOrder(List.of(itemA(2)));
        assertThrows(ReturnAlreadyExistsException.class,
                () -> service(order, staffUser(), List.of(receivedReturn(ITEM_A_ID, 1)), new CapturingSavePort())
                        .execute(command(ITEM_A_ID, 1)));
    }

    @Test
    @DisplayName("Previous SENT_TO_WAREHOUSE return for a different item → succeeds")
    void previousSentReturn_differentItem_succeeds() {
        var save  = new CapturingSavePort();
        var order = deliveredPickpointOrder(List.of(itemA(1), itemB(1)));
        service(order, staffUser(), List.of(sentReturn(ITEM_A_ID, 1)), save)
                .execute(command(ITEM_B_ID, 1));

        assertEquals(CustomerReturnStatus.RECEIVED, save.last().getStatus());
    }

    @Test
    @DisplayName("Item-level guard: item A qty 1 already returned (SENT), second tries qty 1 of A → ReturnItemQuantityExceededException")
    void itemAlreadyFullyReturned_throws() {
        var order = deliveredPickpointOrder(List.of(itemA(1)));
        assertThrows(ReturnItemQuantityExceededException.class,
                () -> service(order, staffUser(), List.of(sentReturn(ITEM_A_ID, 1)), new CapturingSavePort())
                        .execute(command(ITEM_A_ID, 1)));
    }

    @Test
    @DisplayName("Item B qty 2; first return takes qty 1; second takes qty 1 → both succeed")
    void partialReturnAcrossTwoVisits_succeeds() {
        var order = deliveredPickpointOrder(List.of(itemB(2)));

        // First return takes 1
        var save1 = new CapturingSavePort();
        service(order, staffUser(), List.of(), save1).execute(command(ITEM_B_ID, 1));
        assertEquals(CustomerReturnStatus.RECEIVED, save1.last().getStatus());

        // First return marked as SENT_TO_WAREHOUSE (simulates real state)
        var firstSent = sentReturn(ITEM_B_ID, 1);

        // Second return takes 1 more
        var save2 = new CapturingSavePort();
        service(order, staffUser(), List.of(firstSent), save2).execute(command(ITEM_B_ID, 1));
        assertEquals(CustomerReturnStatus.RECEIVED, save2.last().getStatus());
    }

    @Test
    @DisplayName("Second return tries qty 2 of item B when only qty 1 remains → ReturnItemQuantityExceededException")
    void secondReturnExceedsRemainingQty_throws() {
        var order      = deliveredPickpointOrder(List.of(itemB(2)));
        var firstSent  = sentReturn(ITEM_B_ID, 1);

        var cmd = new ReceiveCustomerReturnUseCase.Command(ORDER_ID, STAFF_ID, null,
                List.of(new ReceiveCustomerReturnUseCase.ItemCommand(ITEM_B_ID, 2, ReturnReason.DAMAGED, null)));

        assertThrows(ReturnItemQuantityExceededException.class,
                () -> service(order, staffUser(), List.of(firstSent), new CapturingSavePort()).execute(cmd));
    }

    @Test
    @DisplayName("Order not found → ResourceNotFoundException")
    void orderNotFound_throws() {
        assertThrows(ResourceNotFoundException.class,
                () -> service(null, staffUser(), List.of(), new CapturingSavePort())
                        .execute(command(ITEM_A_ID, 1)));
    }

    @Test
    @DisplayName("Valid return → sendCustomerReturnReceivedNotification called with order's userId and orderId")
    void validReturn_sendsNotification() {
        var save  = new CapturingSavePort();
        var notif = new CapturingNotificationPort();
        var order = deliveredPickpointOrder(List.of(itemA(1)));
        service(order, staffUser(), List.of(), save, notif).execute(command(ITEM_A_ID, 1));

        assertEquals(1, notif.returnReceivedCalls.size());
        assertEquals(order.userId(), notif.returnReceivedCalls.get(0)[0]);
        assertEquals(ORDER_ID, notif.returnReceivedCalls.get(0)[1]);
    }
}
