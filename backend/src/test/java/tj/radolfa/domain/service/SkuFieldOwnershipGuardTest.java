package tj.radolfa.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.domain.exception.FieldLockException;
import tj.radolfa.domain.model.UserRole;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests for {@link SkuFieldOwnershipGuard}.
 * No Spring context, no Mockito, no database.
 */
class SkuFieldOwnershipGuardTest {

    private static final Long SELLER_A = 10L;
    private static final Long SELLER_B = 20L;

    @Test
    @DisplayName("ADMIN edits Radolfa-owned SKU (ownerSellerId=null) → allowed")
    void admin_radolfa_allowed() {
        assertDoesNotThrow(() ->
                SkuFieldOwnershipGuard.assertCanEditPriceOrStock(UserRole.ADMIN, null, null));
    }

    @Test
    @DisplayName("ADMIN edits seller-owned SKU → allowed")
    void admin_seller_allowed() {
        assertDoesNotThrow(() ->
                SkuFieldOwnershipGuard.assertCanEditPriceOrStock(UserRole.ADMIN, null, SELLER_A));
    }

    @Test
    @DisplayName("SELLER edits their own SKU → allowed")
    void seller_own_sku_allowed() {
        assertDoesNotThrow(() ->
                SkuFieldOwnershipGuard.assertCanEditPriceOrStock(UserRole.SELLER, SELLER_A, SELLER_A));
    }

    @Test
    @DisplayName("SELLER edits another seller's SKU → FieldLockException")
    void seller_foreign_sku_denied() {
        assertThrows(FieldLockException.class, () ->
                SkuFieldOwnershipGuard.assertCanEditPriceOrStock(UserRole.SELLER, SELLER_A, SELLER_B));
    }

    @Test
    @DisplayName("SELLER edits Radolfa-owned SKU (ownerSellerId=null) → FieldLockException")
    void seller_radolfa_sku_denied() {
        assertThrows(FieldLockException.class, () ->
                SkuFieldOwnershipGuard.assertCanEditPriceOrStock(UserRole.SELLER, SELLER_A, null));
    }

    @Test
    @DisplayName("MANAGER edits any SKU → FieldLockException (price/stock not a manager concern)")
    void manager_denied() {
        // Radolfa-owned
        assertThrows(FieldLockException.class, () ->
                SkuFieldOwnershipGuard.assertCanEditPriceOrStock(UserRole.MANAGER, null, null));
        // Seller-owned
        assertThrows(FieldLockException.class, () ->
                SkuFieldOwnershipGuard.assertCanEditPriceOrStock(UserRole.MANAGER, null, SELLER_A));
    }
}
