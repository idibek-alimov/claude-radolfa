package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.application.ports.in.loyalty.AdjustLoyaltyPointsUseCase.Command;
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

class AdjustLoyaltyPointsServiceTest {

    private static final int  TTL_MONTHS  = 12;
    private static final Long ACTOR_ID    = 99L;
    private static final Long USER_ID     = 1L;

    private FakeLoadUserPort  fakeLoad;
    private FakeSaveUserPort  fakeSave;
    private FakeLedgerPort    fakeLedger;
    private AdjustLoyaltyPointsService service;

    @BeforeEach
    void setUp() {
        fakeLoad   = new FakeLoadUserPort();
        fakeSave   = new FakeSaveUserPort();
        fakeLedger = new FakeLedgerPort();
        LoyaltyLedgerWriter writer = new LoyaltyLedgerWriter(fakeLedger, fakeLedger, new LoyaltyCalculator());
        service = new AdjustLoyaltyPointsService(fakeLoad, fakeSave, writer,
                new LoyaltyRewardProperties(50, TTL_MONTHS));
    }

    // ── Credit (positive delta) ───────────────────────────────────────────────

    @Test
    @DisplayName("Credit: returns the new balance (oldPoints + delta)")
    void credit_returnsNewBalance() {
        fakeLoad.user = userWithPoints(200);

        int result = service.execute(new Command(USER_ID, 50, "goodwill credit", ACTOR_ID));

        assertEquals(250, result);
    }

    @Test
    @DisplayName("Credit: appends one MANUAL_ADJUSTMENT credit row with correct fields")
    void credit_appendsCreditRow() {
        fakeLoad.user = userWithPoints(100);

        service.execute(new Command(USER_ID, 75, "promo compensation", ACTOR_ID));

        assertEquals(1, fakeLedger.appended.size());
        LoyaltyLedgerEntry row = fakeLedger.appended.get(0);
        assertEquals(LoyaltyReason.MANUAL_ADJUSTMENT, row.reason());
        assertEquals(75,       row.delta());
        assertEquals(75,       row.remainingPoints());
        assertEquals(175,      row.balanceAfter());
        assertEquals(ACTOR_ID, row.actorUserId());
        assertEquals("promo compensation", row.note());
        assertNull(row.orderId());
    }

    @Test
    @DisplayName("Credit: updates the cached balance on the user")
    void credit_updatesCachedBalance() {
        fakeLoad.user = userWithPoints(300);

        service.execute(new Command(USER_ID, 100, "special reward", ACTOR_ID));

        assertEquals(400, fakeSave.saved.loyalty().points());
    }

    @Test
    @DisplayName("Credit: credit lot carries a future expiry date")
    void credit_lotHasExpiry() {
        fakeLoad.user = userWithPoints(0);

        service.execute(new Command(USER_ID, 50, "birthday bonus", ACTOR_ID));

        assertNotNull(fakeLedger.appended.get(0).expiresAt());
        assertTrue(fakeLedger.appended.get(0).expiresAt().isAfter(Instant.now()));
    }

    // ── Debit (negative delta) ────────────────────────────────────────────────

    @Test
    @DisplayName("Debit: returns the new balance (oldPoints - |delta|)")
    void debit_returnsNewBalance() {
        fakeLoad.user = userWithPoints(200);
        fakeLedger.seedLiveLot(10L, 200);

        int result = service.execute(new Command(USER_ID, -80, "correction", ACTOR_ID));

        assertEquals(120, result);
    }

    @Test
    @DisplayName("Debit: FIFO debit row carries the note and actor id")
    void debit_rowHasNoteAndActor() {
        fakeLoad.user = userWithPoints(100);
        fakeLedger.seedLiveLot(10L, 100);

        service.execute(new Command(USER_ID, -40, "admin correction", ACTOR_ID));

        assertFalse(fakeLedger.appended.isEmpty());
        LoyaltyLedgerEntry row = fakeLedger.appended.get(0);
        assertEquals(LoyaltyReason.MANUAL_ADJUSTMENT, row.reason());
        assertEquals(-40,      row.delta());
        assertEquals(ACTOR_ID, row.actorUserId());
        assertEquals("admin correction", row.note());
    }

    @Test
    @DisplayName("Debit: updates the cached balance on the user")
    void debit_updatesCachedBalance() {
        fakeLoad.user = userWithPoints(150);
        fakeLedger.seedLiveLot(10L, 150);

        service.execute(new Command(USER_ID, -50, "balance fix", ACTOR_ID));

        assertEquals(100, fakeSave.saved.loyalty().points());
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delta == 0 is rejected with IllegalArgumentException")
    void zeroDelta_throws() {
        fakeLoad.user = userWithPoints(100);

        assertThrows(IllegalArgumentException.class,
                () -> service.execute(new Command(USER_ID, 0, "no-op", ACTOR_ID)));
    }

    @Test
    @DisplayName("Debit larger than balance is rejected (no silent floor)")
    void debit_overdraw_throws() {
        fakeLoad.user = userWithPoints(50);

        assertThrows(IllegalArgumentException.class,
                () -> service.execute(new Command(USER_ID, -100, "overdraw attempt", ACTOR_ID)));
    }

    @Test
    @DisplayName("Debit exactly equal to balance is accepted (zero result)")
    void debit_exactBalance_succeeds() {
        fakeLoad.user = userWithPoints(75);
        fakeLedger.seedLiveLot(10L, 75);

        int result = service.execute(new Command(USER_ID, -75, "full balance clear", ACTOR_ID));

        assertEquals(0, result);
        assertEquals(0, fakeSave.saved.loyalty().points());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static User userWithPoints(int points) {
        return new User(USER_ID, new PhoneNumber("992000000000"),
                UserRole.USER, "Frank", null,
                new LoyaltyProfile(null, points, null, null, null, false, null),
                true, 1L);
    }

    // ── Fakes ────────────────────────────────────────────────────────────────

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
        final List<LoyaltyLedgerEntry> appended  = new ArrayList<>();
        final List<LoyaltyLedgerEntry> liveLots  = new ArrayList<>();
        private long nextId = 100L;

        /** Seed a live credit lot for FIFO debit tests. */
        void seedLiveLot(Long id, int remaining) {
            liveLots.add(new LoyaltyLedgerEntry(
                    id, USER_ID, remaining, LoyaltyReason.EARN_CASHBACK,
                    null, null, null, remaining, null, remaining, Instant.now()));
        }

        @Override
        public LoyaltyLedgerEntry append(LoyaltyLedgerEntry entry) {
            // Preserve note and all fields; assign an id
            LoyaltyLedgerEntry saved = new LoyaltyLedgerEntry(
                    nextId++, entry.userId(), entry.delta(), entry.reason(),
                    entry.orderId(), entry.actorUserId(), entry.note(),
                    entry.sourceLotId(), entry.remainingPoints(),
                    entry.expiresAt(), entry.balanceAfter(), Instant.now());
            appended.add(saved);
            return saved;
        }

        @Override public void updateRemaining(Long lotId, int newRemaining) {}

        @Override public Page<LoyaltyLedgerEntry> findByUserId(Long userId, Pageable p) { return Page.empty(); }
        @Override public List<LoyaltyLedgerEntry> findLiveLots(Long userId)             { return List.copyOf(liveLots); }
        @Override public List<LoyaltyLedgerEntry> findExpiredLots(Instant now)          { return List.of(); }
    }
}
