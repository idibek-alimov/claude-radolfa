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
import tj.radolfa.infrastructure.config.LoyaltyRewardProperties;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RestoreLoyaltyPointsServiceTest {

    private static final int TTL_MONTHS = 12;

    private FakeLoadUserPort      fakeLoad;
    private FakeSaveUserPort      fakeSave;
    private FakeLedgerPort        fakeLedger;
    private RestoreLoyaltyPointsService service;

    @BeforeEach
    void setUp() {
        fakeLoad   = new FakeLoadUserPort();
        fakeSave   = new FakeSaveUserPort();
        fakeLedger = new FakeLedgerPort();
        LoyaltyLedgerWriter writer = new LoyaltyLedgerWriter(fakeLedger, fakeLedger, new LoyaltyCalculator());
        service = new RestoreLoyaltyPointsService(fakeLoad, fakeSave, writer,
                new LoyaltyRewardProperties(50, TTL_MONTHS));
    }

    @Test
    @DisplayName("Adds restored points to the user's cached balance")
    void execute_incrementsBalance() {
        fakeLoad.user = userWithPoints(100);

        service.execute(1L, 50);

        assertEquals(150, fakeSave.saved.loyalty().points());
    }

    @Test
    @DisplayName("Appends one RESTORE credit lot with correct delta and balance_after")
    void execute_appendsCreditRow() {
        fakeLoad.user = userWithPoints(200);

        service.execute(1L, 75);

        assertEquals(1, fakeLedger.appended.size());
        LoyaltyLedgerEntry row = fakeLedger.appended.get(0);
        assertEquals(LoyaltyReason.RESTORE, row.reason());
        assertEquals(75, row.delta());
        assertEquals(75, row.remainingPoints());
        assertEquals(275, row.balanceAfter());
    }

    @Test
    @DisplayName("Credit lot carries an expiry date (non-null expiresAt)")
    void execute_creditLotHasExpiry() {
        fakeLoad.user = userWithPoints(0);

        service.execute(1L, 100);

        LoyaltyLedgerEntry row = fakeLedger.appended.get(0);
        assertNotNull(row.expiresAt(), "restore lot must have an expiry");
        // expiresAt should be roughly TTL_MONTHS ahead of now
        Instant now = Instant.now();
        assertTrue(row.expiresAt().isAfter(now), "expiresAt must be in the future");
    }

    @Test
    @DisplayName("Restoring into a zero balance works correctly")
    void execute_fromZeroBalance() {
        fakeLoad.user = userWithPoints(0);

        service.execute(1L, 200);

        assertEquals(200, fakeSave.saved.loyalty().points());
        assertEquals(200, fakeLedger.appended.get(0).balanceAfter());
    }

    @Test
    @DisplayName("Throws when pointsToRestore is zero or negative")
    void execute_nonPositive_throws() {
        fakeLoad.user = userWithPoints(100);

        assertThrows(IllegalArgumentException.class, () -> service.execute(1L, 0));
        assertThrows(IllegalArgumentException.class, () -> service.execute(1L, -1));
    }

    @Test
    @DisplayName("All other loyalty profile fields are preserved after restore")
    void execute_preservesOtherFields() {
        fakeLoad.user = userWithPoints(50);

        service.execute(1L, 10);

        User saved = fakeSave.saved;
        assertNull(saved.loyalty().tier());
        assertNull(saved.loyalty().spendToNextTier());
        assertFalse(saved.loyalty().permanent());
    }

    // =========================================================
    //  Helpers
    // =========================================================

    private static User userWithPoints(int points) {
        return new User(1L, new PhoneNumber("992000000000"),
                UserRole.USER, "Diana", null,
                new LoyaltyProfile(null, points, null, null, null, false, null),
                true, 1L);
    }

    // =========================================================
    //  In-memory fakes
    // =========================================================

    static class FakeLoadUserPort implements LoadUserPort {
        User user;
        @Override public Optional<User> loadById(Long id) { return Optional.ofNullable(user); }
        @Override public Optional<User> loadByPhone(String p) { return Optional.empty(); }
        @Override public List<User> findAllNonPermanent() { return List.of(); }
        @Override public List<User> findByRoleAndEnabledTrue(UserRole r) { return List.of(); }
    }

    static class FakeSaveUserPort implements SaveUserPort {
        User saved;
        @Override public User save(User u) { saved = u; return u; }
    }

    static class FakeLedgerPort implements SaveLoyaltyLedgerPort, LoadLoyaltyLedgerPort {
        final List<LoyaltyLedgerEntry> appended = new ArrayList<>();
        private long nextId = 100L;

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
        public void updateRemaining(Long lotId, int newRemaining) { /* no-op — restore never debits */ }

        @Override
        public Page<LoyaltyLedgerEntry> findByUserId(Long userId, Pageable pageable) { return Page.empty(); }

        @Override
        public List<LoyaltyLedgerEntry> findLiveLots(Long userId) { return List.of(); }

        @Override
        public List<LoyaltyLedgerEntry> findExpiredLots(Instant now) { return List.of(); }
    }
}
