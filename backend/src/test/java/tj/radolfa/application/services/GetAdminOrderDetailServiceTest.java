package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.order.GetAdminOrderDetailUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadPickpointPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.Pickpoint;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GetAdminOrderDetailServiceTest {

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static final User FAKE_USER = new User(
            1L, new PhoneNumber("992000000001"), UserRole.USER,
            "Bob", null, LoyaltyProfile.empty(), true, 1L);

    static Order homeOrder() {
        return new Order(10L, 1L, null, OrderStatus.PAID,
                new Money(BigDecimal.valueOf(200)), List.of(),
                Instant.now(), 0, 0,
                DeliveryType.HOME, "Main St 1", "MORNING", null,
                null, null, null,
                null, null, null, null);
    }

    static Order pickpointOrder(Long ppId) {
        return new Order(11L, 1L, null, OrderStatus.PENDING,
                new Money(BigDecimal.valueOf(150)), List.of(),
                Instant.now(), 0, 0,
                DeliveryType.PICKPOINT, null, null, ppId,
                null, null, null,
                null, null, null, null);
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

    static LoadUserPort userPort() {
        return new LoadUserPort() {
            @Override public Optional<User> loadById(Long id) { return Optional.of(FAKE_USER); }
            @Override public Optional<User> loadByPhone(String p) { return Optional.empty(); }
            @Override public List<User> findAllNonPermanent() { return List.of(); }
        };
    }

    static LoadPickpointPort pickpointPort(Optional<Pickpoint> result) {
        return new LoadPickpointPort() {
            @Override public List<Pickpoint> findAll(String search) { return List.of(); }
            @Override public List<Pickpoint> findAllActive() { return List.of(); }
            @Override public Optional<Pickpoint> findById(Long id) { return result; }
        };
    }

    static GetAdminOrderDetailService service(Order order, Optional<Pickpoint> pp) {
        return new GetAdminOrderDetailService(orderPort(order), pickpointPort(pp), userPort());
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("HOME order — no pickpoint lookup, user resolved")
    void homeOrder_noPickpointReturned() {
        GetAdminOrderDetailService svc = service(homeOrder(), Optional.empty());
        GetAdminOrderDetailUseCase.Result r = svc.execute(10L);

        assertEquals(10L, r.order().id());
        assertTrue(r.pickpoint().isEmpty());
        assertEquals("992000000001", r.userPhone());
        assertEquals("Bob", r.userName());
    }

    @Test
    @DisplayName("PICKPOINT order — active pickpoint resolved")
    void pickpointOrder_pickpointResolved() {
        Pickpoint pp = new Pickpoint(99L, "Central", "1 Hub Ave", true);
        GetAdminOrderDetailService svc = service(pickpointOrder(99L), Optional.of(pp));
        GetAdminOrderDetailUseCase.Result r = svc.execute(11L);

        assertEquals(11L, r.order().id());
        assertTrue(r.pickpoint().isPresent());
        assertEquals("Central", r.pickpoint().get().name());
    }

    @Test
    @DisplayName("PICKPOINT order — pickpoint not found in DB still returns result with empty pickpoint")
    void pickpointOrder_pickpointMissing_returnsEmptyOptional() {
        GetAdminOrderDetailService svc = service(pickpointOrder(99L), Optional.empty());
        GetAdminOrderDetailUseCase.Result r = svc.execute(11L);

        assertEquals(11L, r.order().id());
        assertTrue(r.pickpoint().isEmpty());
    }

    @Test
    @DisplayName("Order not found — throws ResourceNotFoundException")
    void orderNotFound_throws() {
        GetAdminOrderDetailService svc = service(null, Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> svc.execute(999L));
    }
}
