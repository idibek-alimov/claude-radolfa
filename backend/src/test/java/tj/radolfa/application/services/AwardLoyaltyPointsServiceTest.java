package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.application.ports.out.LoadLoyaltyLedgerPort;
import tj.radolfa.application.ports.out.LoadLoyaltyTierPort;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveLoyaltyLedgerPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.model.LoyaltyLedgerEntry;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.LoyaltyReason;
import tj.radolfa.domain.model.LoyaltyTier;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;
import tj.radolfa.domain.service.LoyaltyCalculator;
import tj.radolfa.infrastructure.config.LoyaltyRewardProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AwardLoyaltyPointsServiceTest {

    private static final int TTL_MONTHS = 12;

    // Silver tier: cashback 3%, min spend 500
    private static final LoyaltyTier SILVER = new LoyaltyTier(
            1L, "Silver", new BigDecimal("3"), new BigDecimal("3"),
            new BigDecimal("500"), 1, "#C0C0C0");

    private FakeLoadUserPort      fakeLoadUser;
    private FakeLoadOrderPort     fakeLoadOrder;
    private FakeSaveUserPort      fakeSaveUser;
    private FakeSaveOrderPort     fakeSaveOrder;
    private FakeLedgerPort        fakeLedger;
    private AwardLoyaltyPointsService service;

    @BeforeEach
    void setUp() {
        fakeLoadUser  = new FakeLoadUserPort();
        fakeLoadOrder = new FakeLoadOrderPort();
        fakeSaveUser  = new FakeSaveUserPort();
        fakeSaveOrder = new FakeSaveOrderPort();
        fakeLedger    = new FakeLedgerPort();

        LoyaltyCalculator    calculator = new LoyaltyCalculator();
        LoyaltyLedgerWriter  writer     = new LoyaltyLedgerWriter(fakeLedger, fakeLedger, calculator);
        LoyaltyRewardProperties props   = new LoyaltyRewardProperties(50, TTL_MONTHS);

        service = new AwardLoyaltyPointsService(
                fakeLoadUser, fakeLoadOrder, new FakeLoadLoyaltyTierPort(),
                fakeSaveUser, fakeSaveOrder, calculator, writer, props);
    }

    @Test
    @DisplayName("Awards cashback points and appends EARN_CASHBACK lot for a Silver-tier user")
    void execute_silverTier_awardsCorrectPoints() {
        // Silver cashback = 3%; order 1000 TJS → 30 pts
        fakeLoadUser.user   = userWithSilverAndPoints(0);
        fakeLoadOrder.order = order(1L, 1000, 0); // no prior award

        service.execute(1L, 1L);

        // Cached balance updated on user
        assertEquals(30, fakeSaveUser.saved.loyalty().points());
        // Ledger row appended
        assertEquals(1, fakeLedger.appended.size());
        LoyaltyLedgerEntry row = fakeLedger.appended.get(0);
        assertEquals(LoyaltyReason.EARN_CASHBACK, row.reason());
        assertEquals(30, row.delta());
        assertEquals(30, row.balanceAfter());
        assertNotNull(row.expiresAt(), "credit lot must have expiresAt");
        // Order records awarded points
        assertEquals(30, fakeSaveOrder.saved.loyaltyPointsAwarded());
    }

    @Test
    @DisplayName("Idempotency guard: no ledger row written if order already awarded")
    void execute_alreadyAwarded_noLedgerRowAndNoUserSave() {
        fakeLoadUser.user   = userWithSilverAndPoints(30);
        fakeLoadOrder.order = order(1L, 1000, 30); // already awarded

        service.execute(1L, 1L);

        assertNull(fakeSaveUser.saved,  "user must not be re-saved");
        assertNull(fakeSaveOrder.saved, "order must not be re-saved");
        assertTrue(fakeLedger.appended.isEmpty(), "no ledger row on re-entry");
    }

    @Test
    @DisplayName("No ledger row when earned cashback is zero (no tier or 0% cashback)")
    void execute_zeroCashback_noLedgerRow() {
        // User has no tier → 0% cashback
        fakeLoadUser.user   = userWithNoTierAndPoints(100);
        fakeLoadOrder.order = order(1L, 500, 0);

        service.execute(1L, 1L);

        assertTrue(fakeLedger.appended.isEmpty(), "skip noise row when earnedPoints == 0");
        // User saved (tier may have been upgraded), but points unchanged
        assertEquals(100, fakeSaveUser.saved.loyalty().points());
    }

    @Test
    @DisplayName("Order's loyaltyPointsAwarded is recorded even when cashback is zero")
    void execute_zeroCashback_recordsZeroOnOrder() {
        fakeLoadUser.user   = userWithNoTierAndPoints(0);
        fakeLoadOrder.order = order(1L, 200, 0);

        service.execute(1L, 1L);

        // Order should record 0 so the idempotency guard works next time
        assertNotNull(fakeSaveOrder.saved);
        assertEquals(0, fakeSaveOrder.saved.loyaltyPointsAwarded());
    }

    @Test
    @DisplayName("expiresAt on the credit lot is approximately TTL_MONTHS in the future")
    void execute_creditLot_hasCorrectExpiry() {
        fakeLoadUser.user   = userWithSilverAndPoints(0);
        fakeLoadOrder.order = order(1L, 1000, 0);
        Instant before = Instant.now();

        service.execute(1L, 1L);

        Instant after     = Instant.now();
        Instant expiresAt = fakeLedger.appended.get(0).expiresAt();
        assertNotNull(expiresAt);
        assertTrue(expiresAt.isAfter(before), "expiresAt must be in the future");
        // Sanity: must be at least 11 months away and at most 13 months away
        long secondsIn11Mo = 11L * 30 * 24 * 3600;
        long secondsIn13Mo = 13L * 30 * 24 * 3600;
        assertTrue(expiresAt.getEpochSecond() - before.getEpochSecond() > secondsIn11Mo);
        assertTrue(expiresAt.getEpochSecond() - before.getEpochSecond() < secondsIn13Mo);
    }

    // =========================================================
    //  Helpers
    // =========================================================

    private static User userWithSilverAndPoints(int points) {
        return new User(1L, new PhoneNumber("992000000000"),
                UserRole.USER, "Eve", null,
                new LoyaltyProfile(SILVER, points, null, null,
                        new BigDecimal("800"), false, SILVER),
                true, 1L);
    }

    private static User userWithNoTierAndPoints(int points) {
        return new User(1L, new PhoneNumber("992000000000"),
                UserRole.USER, "Eve", null,
                new LoyaltyProfile(null, points, null, null, null, false, null),
                true, 1L);
    }

    private static Order order(Long id, int totalTjs, int alreadyAwarded) {
        return new Order.Builder()
                .id(id)
                .userId(1L)
                .status(OrderStatus.PAID)
                .totalAmount(new Money(new BigDecimal(totalTjs)))
                .loyaltyPointsAwarded(alreadyAwarded)
                .loyaltyPointsRedeemed(0)
                .build();
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

    static class FakeLoadOrderPort implements LoadOrderPort {
        Order order;
        @Override public Optional<Order> loadById(Long id) { return Optional.ofNullable(order); }
        @Override public Optional<Order> loadByExternalOrderId(String ext) { return Optional.empty(); }
        @Override public List<Order> loadByUserId(Long userId) { return List.of(); }
        @Override public List<Order> loadRecentPaidByUserId(Long userId, int limit) { return List.of(); }
    }

    static class FakeLoadLoyaltyTierPort implements LoadLoyaltyTierPort {
        @Override public Optional<LoyaltyTier> findById(Long id) { return Optional.empty(); }
        @Override public Optional<LoyaltyTier> findByName(String n) { return Optional.empty(); }
        @Override public List<LoyaltyTier> findAll() { return List.of(SILVER); }
    }

    static class FakeSaveUserPort implements SaveUserPort {
        User saved;
        @Override public User save(User u) { saved = u; return u; }
    }

    static class FakeSaveOrderPort implements SaveOrderPort {
        Order saved;
        @Override public Order save(Order o) { saved = o; return o; }
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
        public void updateRemaining(Long lotId, int newRemaining) { /* no-op */ }

        @Override
        public Page<LoyaltyLedgerEntry> findByUserId(Long userId, Pageable pageable) { return Page.empty(); }

        @Override
        public List<LoyaltyLedgerEntry> findLiveLots(Long userId) { return List.of(); }

        @Override
        public List<LoyaltyLedgerEntry> findExpiredLots(Instant now) { return List.of(); }
    }
}
