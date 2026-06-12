package tj.radolfa.domain.service;

import tj.radolfa.domain.exception.FieldLockException;
import tj.radolfa.domain.model.UserRole;

/**
 * Pure-domain guard for SKU price/stock edits.
 *
 * <p>Rules:
 * <ul>
 *   <li>ADMIN — may edit any SKU (short-circuit).</li>
 *   <li>Radolfa-owned SKU (ownerSellerId == null) — ADMIN only; everyone else is denied.</li>
 *   <li>SELLER — may edit SKUs of products they own (requesterSellerId == ownerSellerId).</li>
 *   <li>All other cases — denied.</li>
 * </ul>
 *
 * <p>No Spring or JPA imports — pure Java so it lives in the domain layer.
 */
public final class SkuFieldOwnershipGuard {

    private SkuFieldOwnershipGuard() {}

    /**
     * Asserts that the caller may modify price or stock on a SKU.
     *
     * @param role               caller's role
     * @param requesterSellerId  caller's seller profile id (null if not a seller)
     * @param ownerSellerId      product's seller_id (null = Radolfa-owned)
     * @throws FieldLockException if the caller is not permitted
     */
    public static void assertCanEditPriceOrStock(UserRole role,
                                                 Long requesterSellerId,
                                                 Long ownerSellerId) {
        if (role == UserRole.ADMIN) return;

        if (ownerSellerId == null) {
            // Radolfa-owned products are ADMIN-only — everyone else is locked out
            throw new FieldLockException("price/stock", "Radolfa-owned product is ADMIN-only");
        }

        if (role == UserRole.SELLER && ownerSellerId.equals(requesterSellerId)) return;

        throw new FieldLockException("price/stock", "Seller may only edit SKUs of their own products");
    }
}
