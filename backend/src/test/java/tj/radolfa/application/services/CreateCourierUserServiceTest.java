package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.user.CreateCourierUserUseCase.Command;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.exception.DuplicateResourceException;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;
import tj.radolfa.domain.model.VehicleType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class CreateCourierUserServiceTest {

    static final Command VALID_COMMAND = new Command(
            "+992000000001", "Ali Courier", VehicleType.MOTORCYCLE,
            BigDecimal.valueOf(30), 150, 80, 80);

    static class FakeLoadUserPort implements LoadUserPort {
        private User stored;

        void store(User u) { this.stored = u; }

        @Override public Optional<User> loadByPhone(String phone) {
            return stored != null && stored.phone().value().equals(phone)
                    ? Optional.of(stored) : Optional.empty();
        }
        @Override public Optional<User> loadById(Long id) { return Optional.empty(); }
        @Override public List<User> findAllNonPermanent() { return List.of(); }
        @Override public List<User> findByRoleAndEnabledTrue(UserRole role) { return List.of(); }
    }

    static class FakeSaveUserPort implements SaveUserPort {
        private final AtomicLong seq = new AtomicLong(1);
        User lastSaved;

        @Override public User save(User user) {
            lastSaved = new User(seq.getAndIncrement(), user.phone(), user.role(),
                    user.name(), user.email(), user.loyalty(), user.enabled(), 0L,
                    user.vehicleType(), user.maxPayloadKg(), user.maxLengthCm(),
                    user.maxWidthCm(), user.maxHeightCm(), user.pickpointId(), user.deliveryZoneId());
            return lastSaved;
        }
    }

    @Test
    @DisplayName("Happy path — courier user created, returns new id with COURIER role")
    void createCourier_success() {
        var load = new FakeLoadUserPort();
        var save = new FakeSaveUserPort();
        var svc  = new CreateCourierUserService(load, save);

        Long id = svc.execute(VALID_COMMAND);

        assertNotNull(id);
        assertEquals(UserRole.COURIER, save.lastSaved.role());
        assertEquals(VehicleType.MOTORCYCLE, save.lastSaved.vehicleType());
        assertEquals(BigDecimal.valueOf(30), save.lastSaved.maxPayloadKg());
        assertTrue(save.lastSaved.enabled());
    }

    @Test
    @DisplayName("Duplicate phone → DuplicateResourceException")
    void createCourier_duplicatePhone_throws() {
        var load = new FakeLoadUserPort();
        load.store(new User(1L, new PhoneNumber("+992000000001"), UserRole.USER,
                "Existing", null, LoyaltyProfile.empty(), true, 0L));
        var svc = new CreateCourierUserService(load, new FakeSaveUserPort());

        assertThrows(DuplicateResourceException.class, () -> svc.execute(VALID_COMMAND));
    }
}
