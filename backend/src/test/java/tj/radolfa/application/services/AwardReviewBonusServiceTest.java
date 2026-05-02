package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;
import tj.radolfa.infrastructure.config.LoyaltyRewardProperties;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AwardReviewBonusServiceTest {

    private FakeLoadUserPort fakeLoad;
    private FakeSaveUserPort fakeSave;
    private AwardReviewBonusService service;

    private static final int REWARD = 50;

    @BeforeEach
    void setUp() {
        fakeLoad = new FakeLoadUserPort();
        fakeSave = new FakeSaveUserPort();
        service  = new AwardReviewBonusService(fakeLoad, fakeSave, new LoyaltyRewardProperties(REWARD));
    }

    @Test
    @DisplayName("Increments user loyalty points by the configured review reward amount")
    void execute_incrementsPointsByConfiguredAmount() {
        fakeLoad.user = userWithPoints(100);

        service.execute(1L);

        assertNotNull(fakeSave.saved);
        assertEquals(150, fakeSave.saved.loyalty().points());
    }

    @Test
    @DisplayName("Works correctly when user currently has zero points")
    void execute_fromZeroPoints_setsRewardAmount() {
        fakeLoad.user = userWithPoints(0);

        service.execute(1L);

        assertEquals(REWARD, fakeSave.saved.loyalty().points());
    }

    @Test
    @DisplayName("All other loyalty profile fields are preserved after award")
    void execute_preservesOtherLoyaltyFields() {
        fakeLoad.user = userWithPoints(200);

        service.execute(1L);

        User saved = fakeSave.saved;
        assertNull(saved.loyalty().tier());
        assertNull(saved.loyalty().spendToNextTier());
        assertFalse(saved.loyalty().permanent());
    }

    @Test
    @DisplayName("Throws when user is not found")
    void execute_userNotFound_throws() {
        fakeLoad.user = null; // no user

        assertThrows(IllegalStateException.class, () -> service.execute(99L));
        assertNull(fakeSave.saved);
    }

    // =========================================================
    //  Helpers
    // =========================================================

    private static User userWithPoints(int points) {
        return new User(1L, new PhoneNumber("992000000000"),
                UserRole.USER, "Alice", null,
                new LoyaltyProfile(null, points, null, null, null, false, null),
                true, 1L);
    }

    // =========================================================
    //  In-memory fakes
    // =========================================================

    static class FakeLoadUserPort implements LoadUserPort {
        User user;

        @Override
        public Optional<User> loadById(Long id) {
            return Optional.ofNullable(user);
        }

        @Override public Optional<User> loadByPhone(String phone) { return Optional.empty(); }
        @Override public List<User> findAllNonPermanent() { return List.of(); }
    }

    static class FakeSaveUserPort implements SaveUserPort {
        User saved;

        @Override
        public User save(User user) {
            saved = user;
            return user;
        }
    }
}
