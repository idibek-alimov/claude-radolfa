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
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;
import tj.radolfa.domain.service.LoyaltyCalculator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RedeemLoyaltyPointsServiceTest {

    private FakeLoadUserPort      fakeLoad;
    private FakeSaveUserPort      fakeSave;
    private FakeLedgerPort        fakeLedger;
    private RedeemLoyaltyPointsService service;

    @BeforeEach
    void setUp() {
        fakeLoad   = new FakeLoadUserPort();
        fakeSave   = new FakeSaveUserPort();
        fakeLedger = new FakeLedgerPort();
        LoyaltyLedgerWriter writer = new LoyaltyLedgerWriter(fakeLedger, fakeLedger, new LoyaltyCalculator());
        service = new RedeemLoyaltyPointsService(fakeLoad, fakeSave, new LoyaltyCalculator(), writer);
    }

    @Test
    @DisplayName("Returns Money equivalent of redeemed points (1 point = 1 TJS)")
    void execute_returnsCorrectMoneyValue() {
        fakeLoad.user = userWithPoints(200);
        fakeLedger.seedLiveLot(10L, 200);

        Money result = service.execute(1L, 50);

        assertEquals(new BigDecimal("50"), result.amount());
    }

    @Test
    @DisplayName("Deducts the redeemed points from the user's cached balance")
    void execute_deductsBalanceFromUser() {
        fakeLoad.user = userWithPoints(200);
        fakeLedger.seedLiveLot(10L, 200);

        service.execute(1L, 50);

        assertEquals(150, fakeSave.saved.loyalty().points());
    }

    @Test
    @DisplayName("Appends one REDEEM debit row when a single lot covers the full amount")
    void execute_singleLot_appendsOneDebitRow() {
        fakeLoad.user = userWithPoints(200);
        fakeLedger.seedLiveLot(10L, 200);

        service.execute(1L, 100);

        assertEquals(1, fakeLedger.appended.size());
        LoyaltyLedgerEntry row = fakeLedger.appended.get(0);
        assertEquals(LoyaltyReason.REDEEM, row.reason());
        assertEquals(-100, row.delta());
        assertEquals(10L, row.sourceLotId());
        assertEquals(100, row.balanceAfter()); // 200 - 100
    }

    @Test
    @DisplayName("FIFO: two lots touched → two debit rows, oldest lot consumed first")
    void execute_twoLots_appendsTwoDebitRows() {
        fakeLoad.user = userWithPoints(150);
        // Oldest lot has 100 pts, newer lot has 50 pts; redeem 120 → drains lot A + 20 from lot B
        fakeLedger.seedLiveLot(1L, 100);
        fakeLedger.seedLiveLot(2L, 50);

        service.execute(1L, 120);

        assertEquals(2, fakeLedger.appended.size());
        // First debit row — lot 1 fully consumed
        LoyaltyLedgerEntry row1 = fakeLedger.appended.get(0);
        assertEquals(-100, row1.delta());
        assertEquals(1L, row1.sourceLotId());
        // Second debit row — lot 2 partially consumed
        LoyaltyLedgerEntry row2 = fakeLedger.appended.get(1);
        assertEquals(-20, row2.delta());
        assertEquals(2L, row2.sourceLotId());
        // Cached balance after both rows
        assertEquals(30, row2.balanceAfter()); // 150 - 120
    }

    @Test
    @DisplayName("Remaining points on the consumed lot are decremented via updateRemaining")
    void execute_updatesRemainingOnLot() {
        fakeLoad.user = userWithPoints(200);
        fakeLedger.seedLiveLot(10L, 200);

        service.execute(1L, 70);

        assertEquals(130, fakeLedger.updatedRemaining.get(10L)); // 200 - 70
    }

    @Test
    @DisplayName("Throws when points to redeem exceed available balance")
    void execute_insufficientBalance_throws() {
        fakeLoad.user = userWithPoints(50);
        fakeLedger.seedLiveLot(1L, 50);

        assertThrows(IllegalArgumentException.class, () -> service.execute(1L, 100));
        assertNull(fakeSave.saved);
    }

    @Test
    @DisplayName("Throws when pointsToRedeem is zero or negative")
    void execute_nonPositive_throws() {
        fakeLoad.user = userWithPoints(100);

        assertThrows(IllegalArgumentException.class, () -> service.execute(1L, 0));
        assertThrows(IllegalArgumentException.class, () -> service.execute(1L, -1));
    }

    @Test
    @DisplayName("Balance invariant: newBalance == startBalance + SUM(delta) of appended rows")
    void execute_balanceInvariantHolds() {
        int startBalance = 300;
        fakeLoad.user = userWithPoints(startBalance);
        fakeLedger.seedLiveLot(1L, startBalance);

        service.execute(1L, startBalance);

        int sumDelta  = fakeLedger.appended.stream().mapToInt(LoyaltyLedgerEntry::delta).sum();
        int endBalance = fakeSave.saved.loyalty().points();
        // Invariant: endBalance = startBalance + SUM(delta)
        assertEquals(startBalance + sumDelta, endBalance);
    }

    // =========================================================
    //  Helpers
    // =========================================================

    private static User userWithPoints(int points) {
        return new User(1L, new PhoneNumber("992000000000"),
                UserRole.USER, "Bob", null,
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
        final java.util.Map<Long, Integer> updatedRemaining = new java.util.HashMap<>();
        private final List<LoyaltyLedgerEntry> liveLots = new ArrayList<>();
        private long nextId = 100L;

        /** Pre-seed a credit lot so findLiveLots returns it. */
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
        public void updateRemaining(Long lotId, int newRemaining) {
            updatedRemaining.put(lotId, newRemaining);
        }

        @Override
        public Page<LoyaltyLedgerEntry> findByUserId(Long userId, Pageable pageable) {
            return Page.empty();
        }

        @Override
        public List<LoyaltyLedgerEntry> findLiveLots(Long userId) {
            return List.copyOf(liveLots);
        }

        @Override
        public List<LoyaltyLedgerEntry> findExpiredLots(Instant now) { return List.of(); }
    }
}
