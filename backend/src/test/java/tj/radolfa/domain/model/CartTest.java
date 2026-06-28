package tj.radolfa.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CartTest {

    private Cart activeCartWithItem() {
        Cart cart = new Cart(42L, 1L, CartStatus.ACTIVE,
                List.of(new CartItem(10L, 2, new Money(new BigDecimal("100.00")))),
                Instant.now(), Instant.now(), null, null);
        return cart;
    }

    // ── reopen() guard ────────────────────────────────────────────────────────

    @Test
    @DisplayName("reopen() on a CHECKED_OUT cart transitions status back to ACTIVE")
    void reopen_fromCheckedOut_becomesActive() {
        Cart cart = activeCartWithItem();
        cart.checkout();
        assertEquals(CartStatus.CHECKED_OUT, cart.getStatus());

        cart.reopen();

        assertEquals(CartStatus.ACTIVE, cart.getStatus());
    }

    @Test
    @DisplayName("reopen() on an ACTIVE cart throws IllegalStateException")
    void reopen_fromActive_throws() {
        Cart cart = activeCartWithItem();
        assertEquals(CartStatus.ACTIVE, cart.getStatus());

        IllegalStateException ex = assertThrows(IllegalStateException.class, cart::reopen);
        assertTrue(ex.getMessage().contains("ACTIVE"));
    }

    @Test
    @DisplayName("reopen() on an ABANDONED cart throws IllegalStateException")
    void reopen_fromAbandoned_throws() {
        Cart cart = activeCartWithItem();
        cart.abandon();
        assertEquals(CartStatus.ABANDONED, cart.getStatus());

        IllegalStateException ex = assertThrows(IllegalStateException.class, cart::reopen);
        assertTrue(ex.getMessage().contains("ABANDONED"));
    }

    @Test
    @DisplayName("reopen() after checkout() allows items to be mutated again")
    void reopen_allowsSubsequentMutation() {
        Cart cart = activeCartWithItem();
        cart.checkout();
        cart.reopen();

        // Must be ACTIVE again — addItem()'s requireActive() should not throw.
        assertDoesNotThrow(() ->
                cart.addItem(99L, 1, new Money(new BigDecimal("50.00"))));
    }
}
