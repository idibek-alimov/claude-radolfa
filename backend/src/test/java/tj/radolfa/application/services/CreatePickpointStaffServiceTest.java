package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.user.CreatePickpointStaffUseCase.Command;
import tj.radolfa.application.ports.out.LoadPickpointPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.exception.DuplicateResourceException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.Pickpoint;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class CreatePickpointStaffServiceTest {

    static final Command VALID = new Command("+992000000002", "Bob Staff", 10L);

    static Pickpoint pickpoint(Long id) {
        return new Pickpoint(id, "PP1", "Addr", true, null, null, false, false, false, false, null, false);
    }

    static class FakeLoadUserPort implements LoadUserPort {
        private User stored;
        void store(User u) { this.stored = u; }
        @Override public Optional<User> loadByPhone(String phone) {
            return stored != null && stored.phone().value().equals(phone)
                    ? Optional.of(stored) : Optional.empty();
        }
        @Override public Optional<User> loadById(Long id) { return Optional.empty(); }
        @Override public List<User> findAllNonPermanent() { return List.of(); }
        @Override public List<User> findByRoleAndEnabledTrue(UserRole r) { return List.of(); }
    }

    static class FakeLoadPickpointPort implements LoadPickpointPort {
        private final Pickpoint pickpoint;
        FakeLoadPickpointPort(Pickpoint p) { this.pickpoint = p; }
        @Override public List<Pickpoint> findAll(String s) { return List.of(); }
        @Override public List<Pickpoint> findAllActive() { return List.of(); }
        @Override public Optional<Pickpoint> findById(Long id) {
            return pickpoint != null && pickpoint.id().equals(id)
                    ? Optional.of(pickpoint) : Optional.empty();
        }
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
    @DisplayName("Happy path — pickpoint staff created with PICKPOINT_STAFF role and pickpointId set")
    void createStaff_success() {
        var save = new FakeSaveUserPort();
        var svc  = new CreatePickpointStaffService(new FakeLoadUserPort(),
                save, new FakeLoadPickpointPort(pickpoint(10L)));

        Long id = svc.execute(VALID);

        assertNotNull(id);
        assertEquals(UserRole.PICKPOINT_STAFF, save.lastSaved.role());
        assertEquals(10L, save.lastSaved.pickpointId());
    }

    @Test
    @DisplayName("Pickpoint not found → ResourceNotFoundException")
    void missingPickpoint_throws() {
        var svc = new CreatePickpointStaffService(new FakeLoadUserPort(),
                new FakeSaveUserPort(), new FakeLoadPickpointPort(null));

        assertThrows(ResourceNotFoundException.class, () -> svc.execute(VALID));
    }

    @Test
    @DisplayName("Duplicate phone → DuplicateResourceException")
    void duplicatePhone_throws() {
        var load = new FakeLoadUserPort();
        load.store(new User(1L, new PhoneNumber("+992000000002"), UserRole.USER,
                "X", null, LoyaltyProfile.empty(), true, 0L));
        var svc = new CreatePickpointStaffService(load, new FakeSaveUserPort(),
                new FakeLoadPickpointPort(pickpoint(10L)));

        assertThrows(DuplicateResourceException.class, () -> svc.execute(VALID));
    }
}
