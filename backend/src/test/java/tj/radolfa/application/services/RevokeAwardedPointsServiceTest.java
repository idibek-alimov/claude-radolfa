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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RevokeAwardedPointsServiceTest {

    private FakeLoadUserPort        fakeLoad;
    private FakeSaveUserPort        fakeSave;
    private FakeLedgerPort          fakeLedger;
    private RevokeAwardedPointsService service;

    @BeforeEach
    void setUp() {
        fakeLoad   = new FakeLoadUserPort();
        fakeSave   = new FakeSaveUserPort();
        fakeLedger = new FakeLedgerPort();
        LoyaltyLedgerWriter writer = new LoyaltyLedgerWriter(fakeLedger, fakeLedger, new LoyaltyCalculator());
        service = new RevokeAwardedPointsService(fakeLoad, fakeSave, writer);
    }

    @Test
    @DisplayName("Deducts the revoked points from the cached balance")
    void execute_deductsBalance() {
        fakeLoad.user = userWithPoints(300);
        fakeLedger.seedLiveLot(1L, 300);

        service.execute(1L, 100);

        assertEquals(200, fakeSave.saved.loyalty().points());
    }

    @Test
    @DisplayName("Appends a REVOKE debit row for the consumed lot")
    void execute_appendsRevokeDebitRow() {
        fakeLoad.user = userWithPoints(200);
        fakeLedger.seedLiveLot(1L, 200);

        service.execute(1L, 50);

        assertEquals(1, fakeLedger.appended.size());
        LoyaltyLedgerEntry row = fakeLedger.appended.get(0);
        assertEquals(LoyaltyReason.REVOKE, row.reason());
        assertEquals(-50, row.delta());
        assertEquals(150, row.balanceAfter());
    }

    @Test
    @DisplayName("Balance floors at zero when revoking more than available")
    void execute_revokeBeyondBalance_floorsAtZero() {
        fakeLoad.user = userWithPoints(30);
        fakeLedger.seedLiveLot(1L, 30);

        service.execute(1L, 100); // revoke 100, only 30 available

        assertEquals(0, fakeSave.saved.loyalty().points());
        // Only 30 consumed, one REVOKE row for lot 1
        assertEquals(1, fakeLedger.appended.size());
        assertEquals(-30, fakeLedger.appended.get(0).delta());
    }

    @Test
    @DisplayName("No ledger row when balance is already zero")
    void execute_zeroBalance_noLedgerRow() {
        fakeLoad.user = userWithPoints(0);

        service.execute(1L, 50); // revoke 50, but nothing to consume

        assertEquals(0, fakeSave.saved.loyalty().points());
        assertTrue(fakeLedger.appended.isEmpty(), "no ledger row when balance is zero");
    }

    @Test
    @DisplayName("FIFO: revoke spanning two lots produces two REVOKE rows")
    void execute_fifo_twoLots() {
        fakeLoad.user = userWithPoints(150);
        fakeLedger.seedLiveLot(1L, 100); // oldest
        fakeLedger.seedLiveLot(2L, 50);

        service.execute(1L, 120);

        assertEquals(2, fakeLedger.appended.size());
        assertEquals(-100, fakeLedger.appended.get(0).delta());
        assertEquals(-20, fakeLedger.appended.get(1).delta());
        assertEquals(30, fakeSave.saved.loyalty().points()); // 150 - 120
    }

    @Test
    @DisplayName("Throws when pointsToRevoke is zero or negative")
    void execute_nonPositive_throws() {
        fakeLoad.user = userWithPoints(100);

        assertThrows(IllegalArgumentException.class, () -> service.execute(1L, 0));
        assertThrows(IllegalArgumentException.class, () -> service.execute(1L, -5));
    }

    // =========================================================
    //  Helpers
    // =========================================================

    private static User userWithPoints(int points) {
        return new User(1L, new PhoneNumber("992000000000"),
                UserRole.USER, "Charlie", null,
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
        private final List<LoyaltyLedgerEntry> liveLots = new ArrayList<>();
        private long nextId = 100L;

        void seedLiveLot(Long id, int remaining) {
            liveLots.add(new LoyaltyLedgerEntry(
                    id, 1L, remaining, LoyaltyReason.EARN_CASHBACK,
                    null, null, null, remaining, null, remaining, Instant.now()));
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
        public void updateRemaining(Long lotId, int newRemaining) { /* captured but not asserted here */ }

        @Override
        public Page<LoyaltyLedgerEntry> findByUserId(Long userId, Pageable pageable) { return Page.empty(); }

        @Override
        public List<LoyaltyLedgerEntry> findLiveLots(Long userId) { return List.copyOf(liveLots); }

        @Override
        public List<LoyaltyLedgerEntry> findExpiredLots(Instant now) { return List.of(); }
    }
}
