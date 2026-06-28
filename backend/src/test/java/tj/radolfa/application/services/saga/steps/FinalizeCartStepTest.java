package tj.radolfa.application.services.saga.steps;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadCartPort;
import tj.radolfa.application.ports.out.SaveCartPort;
import tj.radolfa.application.services.saga.PaymentConfirmationContext;
import tj.radolfa.domain.model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FinalizeCartStepTest {

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static class FakeLoadCartPort implements LoadCartPort {
        private final Map<Long, Cart> store;

        FakeLoadCartPort(Map<Long, Cart> store) { this.store = store; }

        @Override public Optional<Cart> findActiveByUserId(Long userId)        { return Optional.empty(); }
        @Override public Optional<Cart> findById(Long cartId)                  { return Optional.ofNullable(store.get(cartId)); }
        @Override public Optional<Cart> findByPendingOrderId(Long orderId)     {
            return store.values().stream()
                    .filter(c -> orderId.equals(c.getPendingOrderId()))
                    .findFirst();
        }
    }

    /** Save overwrites the entry and returns the same cart (id already set). */
    static class FakeSaveCartPort implements SaveCartPort {
        private final Map<Long, Cart> store;

        FakeSaveCartPort(Map<Long, Cart> store) { this.store = store; }

        @Override
        public Cart save(Cart cart) {
            store.put(cart.getId(), cart);
            return cart;
        }
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    /** Builds an ACTIVE cart with one item and a pending-order link at the given id. */
    private static Cart activeCart(Long cartId, Long pendingOrderId) {
        return new Cart(cartId, 1L, CartStatus.ACTIVE,
                List.of(new CartItem(10L, 2, new Money(new BigDecimal("100.00")))),
                Instant.now(), Instant.now(), null, pendingOrderId);
    }

    /** Builds a bare context with only the payment reference needed by execute(). */
    private static PaymentConfirmationContext ctxWithOrderId(Long orderId) {
        PaymentConfirmationContext ctx = new PaymentConfirmationContext("tx-test");
        ctx.payment = new Payment(1L, orderId,
                new Money(new BigDecimal("200.00")), "TJS",
                PaymentStatus.PENDING, "payme", null, null,
                Instant.now(), null);
        return ctx;
    }

    private Map<Long, Cart> store;
    private FinalizeCartStep step;

    @BeforeEach
    void setUp() {
        store = new HashMap<>();
        step  = new FinalizeCartStep(new FakeLoadCartPort(store), new FakeSaveCartPort(store));
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("execute() checks out the cart and records cartFinalized + finalizedCartId in context")
    void execute_checksOutCart_populatesContext() {
        Long orderId = 7L;
        store.put(1L, activeCart(1L, orderId));
        PaymentConfirmationContext ctx = ctxWithOrderId(orderId);

        step.execute(ctx);

        assertTrue(ctx.cartFinalized);
        assertEquals(1L, ctx.finalizedCartId);
        assertEquals(CartStatus.CHECKED_OUT, store.get(1L).getStatus());
    }

    @Test
    @DisplayName("compensate() after execute() reopens the cart back to ACTIVE")
    void compensate_afterExecute_reopensCart() {
        Long orderId = 7L;
        store.put(1L, activeCart(1L, orderId));
        PaymentConfirmationContext ctx = ctxWithOrderId(orderId);

        step.execute(ctx);
        assertEquals(CartStatus.CHECKED_OUT, store.get(1L).getStatus());

        step.compensate(ctx);

        assertEquals(CartStatus.ACTIVE, store.get(1L).getStatus());
    }

    @Test
    @DisplayName("compensate() with cartFinalized=false is a no-op (idempotent guard)")
    void compensate_notFinalized_isNoOp() {
        store.put(1L, activeCart(1L, 7L));
        PaymentConfirmationContext ctx = ctxWithOrderId(7L);
        // Do NOT call execute() — cartFinalized stays false.

        assertDoesNotThrow(() -> step.compensate(ctx));
        // Cart must remain untouched (still ACTIVE).
        assertEquals(CartStatus.ACTIVE, store.get(1L).getStatus());
    }

    @Test
    @DisplayName("compensate() with no matching cart is a no-op (defensive ifPresent)")
    void compensate_cartNotFound_isNoOp() {
        PaymentConfirmationContext ctx = ctxWithOrderId(99L);
        ctx.cartFinalized   = true;
        ctx.finalizedCartId = 999L;   // no cart with this id in the store

        assertDoesNotThrow(() -> step.compensate(ctx));
    }
}
