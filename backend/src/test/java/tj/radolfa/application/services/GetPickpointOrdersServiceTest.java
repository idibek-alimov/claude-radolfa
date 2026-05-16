package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadPickpointOrdersPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GetPickpointOrdersServiceTest {

    static User staffUser(Long pickpointId) {
        return new User(5L, new PhoneNumber("+992000000005"), UserRole.PICKPOINT_STAFF,
                "Staff", null, LoyaltyProfile.empty(), true, 0L,
                null, null, null, null, null,
                pickpointId, null);
    }

    static class FakeLoadUserPort implements LoadUserPort {
        final User user;
        FakeLoadUserPort(User u) { this.user = u; }
        @Override public Optional<User> loadById(Long id) {
            return user != null && user.id().equals(id) ? Optional.of(user) : Optional.empty();
        }
        @Override public Optional<User> loadByPhone(String p) { return Optional.empty(); }
        @Override public List<User> findAllNonPermanent() { return List.of(); }
        @Override public List<User> findByRoleAndEnabledTrue(UserRole r) { return List.of(); }
    }

    static class FakeLoadPickpointOrdersPort implements LoadPickpointOrdersPort {
        Long capturedPickpointId;
        OrderStatus capturedStatus;
        List<Order> result = List.of();

        @Override
        public List<Order> loadByPickpointIdAndStatus(Long pickpointId, OrderStatus status) {
            this.capturedPickpointId = pickpointId;
            this.capturedStatus      = status;
            return result;
        }
    }

    @Test
    @DisplayName("Happy path — resolves pickpointId from staff user and queries READY_FOR_PICKUP")
    void execute_success() {
        var port  = new FakeLoadPickpointOrdersPort();
        var load  = new FakeLoadUserPort(staffUser(10L));
        var svc   = new GetPickpointOrdersService(load, port);

        svc.execute(5L);

        assertEquals(10L, port.capturedPickpointId);
        assertEquals(OrderStatus.READY_FOR_PICKUP, port.capturedStatus);
    }

    @Test
    @DisplayName("User not found → ResourceNotFoundException")
    void userNotFound_throws() {
        var svc = new GetPickpointOrdersService(
                new FakeLoadUserPort(null), new FakeLoadPickpointOrdersPort());

        assertThrows(ResourceNotFoundException.class, () -> svc.execute(999L));
    }

    @Test
    @DisplayName("User has null pickpointId → IllegalStateException")
    void nullPickpointId_throws() {
        var svc = new GetPickpointOrdersService(
                new FakeLoadUserPort(staffUser(null)), new FakeLoadPickpointOrdersPort());

        assertThrows(IllegalStateException.class, () -> svc.execute(5L));
    }
}
