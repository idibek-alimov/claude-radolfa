package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.order.BulkReassignOrdersUseCase.Command;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BulkReassignOrdersServiceTest {

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static final User COURIER = new User(
            5L, new PhoneNumber("992000000005"), UserRole.COURIER,
            "Bobur", null, LoyaltyProfile.empty(), true, 1L);

    static Order orderWithStatus(Long id, OrderStatus status) {
        return new Order.Builder()
                .id(id).userId(10L).status(status)
                .totalAmount(new Money(BigDecimal.valueOf(200))).createdAt(Instant.now())
                .deliveryType(DeliveryType.HOME).deliveryAddress("Addr")
                .courierId(99L)
                .build();
    }

    static LoadOrderPort orderPort(Order... orders) {
        Map<Long, Order> map = new HashMap<>();
        for (Order o : orders) map.put(o.id(), o);
        return new LoadOrderPort() {
            @Override public Optional<Order> loadById(Long id) { return Optional.ofNullable(map.get(id)); }
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
    }

    static BulkReassignOrdersService service(LoadOrderPort orderPort,
                                              CapturingSaveOrderPort save,
                                              LoadUserPort userPort) {
        return new BulkReassignOrdersService(orderPort, save, userPort);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Three SHIPPED orders are all reassigned to the new courier")
    void bulkReassign_updatesAllOrders() {
        Order o1 = orderWithStatus(1L, OrderStatus.SHIPPED);
        Order o2 = orderWithStatus(2L, OrderStatus.OUT_FOR_DELIVERY);
        Order o3 = orderWithStatus(3L, OrderStatus.DELIVERY_ATTEMPTED);
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();

        service(orderPort(o1, o2, o3), save, userPort(COURIER))
                .execute(new Command(List.of(1L, 2L, 3L), COURIER.id()));

        assertEquals(3, save.saved.size());
        save.saved.forEach(o -> assertEquals(COURIER.id(), o.courierId()));
    }

    @Test
    @DisplayName("Missing new courier → ResourceNotFoundException, nothing saved")
    void missingCourier_throwsResourceNotFound() {
        Order o = orderWithStatus(1L, OrderStatus.SHIPPED);
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();

        assertThrows(ResourceNotFoundException.class, () ->
                service(orderPort(o), save, userPort(null))
                        .execute(new Command(List.of(1L), 999L)));

        assertTrue(save.saved.isEmpty());
    }

    @Test
    @DisplayName("User exists but is not COURIER role → IllegalStateException")
    void nonCourierUser_throwsIllegalState() {
        User manager = new User(5L, new PhoneNumber("992000000005"), UserRole.MANAGER,
                "Admin", null, LoyaltyProfile.empty(), true, 1L);
        Order o = orderWithStatus(1L, OrderStatus.SHIPPED);
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();

        assertThrows(IllegalStateException.class, () ->
                service(orderPort(o), save, userPort(manager))
                        .execute(new Command(List.of(1L), manager.id())));

        assertTrue(save.saved.isEmpty());
    }

    @Test
    @DisplayName("Order in non-reassignable status (PAID) → IllegalStateException")
    void nonReassignableStatus_throwsIllegalState() {
        Order paid = orderWithStatus(1L, OrderStatus.PAID);
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();

        assertThrows(IllegalStateException.class, () ->
                service(orderPort(paid), save, userPort(COURIER))
                        .execute(new Command(List.of(1L), COURIER.id())));
    }
}
