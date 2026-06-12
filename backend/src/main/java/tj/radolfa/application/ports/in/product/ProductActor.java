package tj.radolfa.application.ports.in.product;

import tj.radolfa.domain.model.UserRole;

/**
 * Actor context for product-lifecycle mutations (submit-for-review, etc.).
 *
 * <p>Similar to {@link SkuEditActor} but product-scoped. The ownership rule here differs:
 * MANAGER is allowed to act on any product (unlike SKU price/stock which is ADMIN/SELLER only),
 * so the guard logic lives in the service rather than a shared domain guard.
 *
 * @param role     the actor's role — drives the ownership check
 * @param userId   the actor's user-table PK — for audit logging
 * @param sellerId the actor's seller-profile PK; {@code null} for MANAGER / ADMIN callers
 */
public record ProductActor(UserRole role, Long userId, Long sellerId) {}
