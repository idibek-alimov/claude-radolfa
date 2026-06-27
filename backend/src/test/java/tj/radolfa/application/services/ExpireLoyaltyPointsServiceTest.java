package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.application.ports.out.LoadLoyaltyLedgerPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveLoyaltyLedgerPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.model.LoyaltyLedgerEntry;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.LoyaltyReason;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;
import tj.radolfa.domain.service.LoyaltyCalculator;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ExpireLoyaltyPointsServiceTest {

    private FakeLedgerPort            fakeLedger;
    private FakeLoadUserPort          fakeLoad;
    private FakeSaveUserPort          fakeSave;
    private ExpireLoyaltyPointsService service;

    @BeforeEach
    void setUp() {
        fakeLedger = new FakeLedgerPort();
        fakeLoad   = new FakeLoadUserPort();
        fakeSave   = new FakeSaveUserPort();

        LoyaltyLedgerWriter      writer      = new LoyaltyLedgerWriter(fakeLedger, fakeLedger, new LoyaltyCalculator());
        UserPointsExpiryService  perUser     = new UserPointsExpiryService(fakeLoad, fakeSave, writer);
        service = new ExpireLoyaltyPointsService(fakeLedger, perUser);
    }

    // =========================================================
    //  Tests
    // =========================================================

    @Test
    @DisplayName("Single expired lot → one EXPIRE row, remaining zeroed, user balance reduced")
    void expireDueLots_singleLot_writesOneExpireRow() {
        fakeLoad.seed(userWithPoints(1L, 200));
        fakeLedger.seedExpiredLot(10L, 1L, 200);

        service.expireDueLots();

        assertEquals(1, fakeLedger.appended.size());
        LoyaltyLedgerEntry row = fakeLedger.appended.get(0);
        assertEquals(LoyaltyReason.EXPIRE, row.reason());
        assertEquals(-200, row.delta());
        assertEquals(10L, row.sourceLotId());
        assertEquals(0, row.balanceAfter());
        assertNull(row.remainingPoints());
        assertNull(row.expiresAt());

        assertEquals(0, fakeLedger.updatedRemaining.get(10L));
        assertEquals(0, fakeSave.saved.get(1L).loyalty().points());
    }

    @Test
    @DisplayName("Two expired lots for one user → two EXPIRE rows with running balance, user saved once")
    void expireDueLots_twoLotsOneUser_twoRows() {
        fakeLoad.seed(userWithPoints(1L, 150));
        fakeLedger.seedExpiredLot(1L, 1L, 100);
        fakeLedger.seedExpiredLot(2L, 1L, 50);

        service.expireDueLots();

        assertEquals(2, fakeLedger.appended.size());
        assertEquals(-100, fakeLedger.appended.get(0).delta());
        assertEquals(50,   fakeLedger.appended.get(0).balanceAfter()); // 150 - 100
        assertEquals(-50,  fakeLedger.appended.get(1).delta());
        assertEquals(0,    fakeLedger.appended.get(1).balanceAfter()); // 50 - 50

        assertEquals(0, fakeLedger.updatedRemaining.get(1L));
        assertEquals(0, fakeLedger.updatedRemaining.get(2L));
        assertEquals(1, fakeSave.saved.size());
        assertEquals(0, fakeSave.saved.get(1L).loyalty().points());
    }

    @Test
    @DisplayName("Expired lots across two users → each user saved with its own reduced balance")
    void expireDueLots_twoUsers_eachSavedIndependently() {
        fakeLoad.seed(userWithPoints(1L, 100));
        fakeLoad.seed(userWithPoints(2L, 300));
        fakeLedger.seedExpiredLot(10L, 1L, 100);
        fakeLedger.seedExpiredLot(20L, 2L, 200);

        service.expireDueLots();

        assertEquals(2, fakeLedger.appended.size());
        assertEquals(2, fakeSave.saved.size());
        assertEquals(0,   fakeSave.saved.get(1L).loyalty().points());
        assertEquals(100, fakeSave.saved.get(2L).loyalty().points()); // 300 - 200
    }

    @Test
    @DisplayName("No expired lots → no rows appended, no user saved")
    void expireDueLots_noneExpired_noWrites() {
        service.expireDueLots();

        assertTrue(fakeLedger.appended.isEmpty());
        assertTrue(fakeSave.saved.isEmpty());
    }

    @Test
    @DisplayName("Balance invariant: endBalance == startBalance + SUM(delta of appended rows)")
    void expireDueLots_balanceInvariantHolds() {
        int startBalance = 250;
        fakeLoad.seed(userWithPoints(1L, startBalance));
        fakeLedger.seedExpiredLot(1L, 1L, 100);
        fakeLedger.seedExpiredLot(2L, 1L, 150);

        service.expireDueLots();

        int sumDelta   = fakeLedger.appended.stream().mapToInt(LoyaltyLedgerEntry::delta).sum();
        int endBalance = fakeSave.saved.get(1L).loyalty().points();
        assertEquals(startBalance + sumDelta, endBalance);
    }

    // =========================================================
    //  Helpers
    // =========================================================

    private static User userWithPoints(Long id, int points) {
        return new User(id, new PhoneNumber("99200000000" + id),
                UserRole.USER, "User" + id, null,
                new LoyaltyProfile(null, points, null, null, null, false, null),
                true, 1L);
    }

    // =========================================================
    //  In-memory fakes
    // =========================================================

    static class FakeLedgerPort implements SaveLoyaltyLedgerPort, LoadLoyaltyLedgerPort {
        final List<LoyaltyLedgerEntry>  appended         = new ArrayList<>();
        final Map<Long, Integer>        updatedRemaining = new HashMap<>();
        private final List<LoyaltyLedgerEntry> expiredLots = new ArrayList<>();
        private long nextId = 100L;

        void seedExpiredLot(Long id, Long userId, int remaining) {
            expiredLots.add(new LoyaltyLedgerEntry(
                    id, userId, remaining, LoyaltyReason.EARN_CASHBACK,
                    null, null, null, remaining,
                    Instant.now().minus(400, ChronoUnit.DAYS), // already expired
                    remaining, Instant.now().minus(400, ChronoUnit.DAYS)));
        }

        @Override
        public LoyaltyLedgerEntry append(LoyaltyLedgerEntry entry) {
            LoyaltyLedgerEntry saved = new LoyaltyLedgerEntry(
                    nextId++, entry.userId(), entry.delta(), entry.reason(),
                    entry.orderId(), entry.actorUserId(), entry.sourceLotId(),
                    entry.remainingPoints(), entry.expiresAt(), entry.balanceAfter(), Instant.now());
            appended.add(saved);
            return saved;
        }

        @Override
        public void updateRemaining(Long lotId, int newRemaining) {
            updatedRemaining.put(lotId, newRemaining);
        }

        @Override
        public Page<LoyaltyLedgerEntry> findByUserId(Long userId, Pageable pageable) {
            return Page.empty();
        }

        @Override
        public List<LoyaltyLedgerEntry> findLiveLots(Long userId) {
            return List.of();
        }

        @Override
        public List<LoyaltyLedgerEntry> findExpiredLots(Instant now) {
            return List.copyOf(expiredLots);
        }
    }

    static class FakeLoadUserPort implements LoadUserPort {
        private final Map<Long, User> users = new HashMap<>();

        void seed(User user) { users.put(user.id(), user); }

        @Override public Optional<User> loadById(Long id)       { return Optional.ofNullable(users.get(id)); }
        @Override public Optional<User> loadByPhone(String p)   { return Optional.empty(); }
        @Override public List<User>     findAllNonPermanent()   { return List.of(); }
        @Override public List<User>     findByRoleAndEnabledTrue(UserRole r) { return List.of(); }
    }

    static class FakeSaveUserPort implements SaveUserPort {
        final Map<Long, User> saved = new HashMap<>();
        @Override public User save(User u) { saved.put(u.id(), u); return u; }
    }
}
