package tj.radolfa.application.services.saga.steps;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.loyalty.AwardLoyaltyPointsUseCase;
import tj.radolfa.application.ports.in.loyalty.RevokeAwardedPointsUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.services.saga.PaymentConfirmationContext;
import tj.radolfa.domain.model.Order;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AwardLoyaltyPointsStepTest {

    // ── Fakes ────────────────────────────────────────────────────────────────

    static class CapturingAwardUseCase implements AwardLoyaltyPointsUseCase {
        final List<Long> calledWithOrderIds = new ArrayList<>();
        @Override public void execute(Long userId, Long orderId) { calledWithOrderIds.add(orderId); }
    }

    static class CapturingRevokeUseCase implements RevokeAwardedPointsUseCase {
        record Call(Long userId, int points) {}
        final List<Call> calls = new ArrayList<>();
        @Override public void execute(Long userId, int pointsToRevoke) {
            calls.add(new Call(userId, pointsToRevoke));
        }
    }

    static class FakeLoadOrderPort implements LoadOrderPort {
        Order order;
        @Override public Optional<Order> loadById(Long id) { return Optional.ofNullable(order); }
        @Override public Optional<Order> loadByExternalOrderId(String ext) { return Optional.empty(); }
        @Override public List<Order> loadByUserId(Long userId) { return List.of(); }
        @Override public List<Order> loadRecentPaidByUserId(Long userId, int limit) { return List.of(); }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Minimal order used as ctx.order (pre-award; awarded = 0). */
    private static Order preAwardOrder() {
        return new Order.Builder().id(10L).userId(42L).loyaltyPointsAwarded(0).build();
    }

    /** Order returned by the port after the award service commits the count. */
    private static Order postAwardOrder(int awarded) {
        return new Order.Builder().id(10L).userId(42L).loyaltyPointsAwarded(awarded).build();
    }

    private static AwardLoyaltyPointsStep step(AwardLoyaltyPointsUseCase award,
                                                RevokeAwardedPointsUseCase revoke,
                                                LoadOrderPort loadOrder) {
        return new AwardLoyaltyPointsStep(award, revoke, loadOrder);
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("execute — calls award use case and captures awarded count from reloaded order")
    void execute_capturesAwardedCount() {
        var award   = new CapturingAwardUseCase();
        var revoke  = new CapturingRevokeUseCase();
        var port    = new FakeLoadOrderPort();
        port.order  = postAwardOrder(75);   // simulates what award service persisted

        var ctx = new PaymentConfirmationContext("tx-001");
        ctx.order = preAwardOrder();

        step(award, revoke, port).execute(ctx);

        assertEquals(1, award.calledWithOrderIds.size(),
                "award use case must be called once");
        assertEquals(10L, award.calledWithOrderIds.get(0),
                "award use case must receive the correct orderId");
        assertTrue(ctx.loyaltyAwarded, "loyaltyAwarded flag must be set");
        assertEquals(75, ctx.awardedPoints,
                "ctx.awardedPoints must reflect the count from the reloaded order");
        assertTrue(revoke.calls.isEmpty(),
                "compensate was not called, revoke must not be invoked");
    }

    @Test
    @DisplayName("compensate — awardedPoints > 0 → revokes correct amount for correct user")
    void compensate_withAwardedPoints_revokesPoints() {
        var award  = new CapturingAwardUseCase();
        var revoke = new CapturingRevokeUseCase();
        var port   = new FakeLoadOrderPort();

        var ctx = new PaymentConfirmationContext("tx-002");
        ctx.order         = preAwardOrder();
        ctx.awardedPoints = 120;   // set as if execute already ran

        step(award, revoke, port).compensate(ctx);

        assertEquals(1, revoke.calls.size(), "revoke must be called exactly once");
        assertEquals(42L, revoke.calls.get(0).userId(), "revoke must target the order's user");
        assertEquals(120, revoke.calls.get(0).points(),  "revoke must pass the exact awarded count");
        assertTrue(award.calledWithOrderIds.isEmpty(),
                "award use case must not be called during compensate");
    }

    @Test
    @DisplayName("compensate — awardedPoints == 0 (zero-cashback order) → revoke is NOT called")
    void compensate_withZeroAwardedPoints_skipsRevoke() {
        var award  = new CapturingAwardUseCase();
        var revoke = new CapturingRevokeUseCase();
        var port   = new FakeLoadOrderPort();

        var ctx = new PaymentConfirmationContext("tx-003");
        ctx.order         = preAwardOrder();
        ctx.awardedPoints = 0;   // zero-cashback: nothing to revoke

        step(award, revoke, port).compensate(ctx);

        assertTrue(revoke.calls.isEmpty(),
                "revoke must be skipped when awardedPoints == 0");
    }
}
