package tj.radolfa.application.ports.in.product;

import tj.radolfa.domain.model.UserRole;

/**
 * Carries the identity of the actor requesting a SKU price or stock edit.
 *
 * @param role      caller's role (used by the ownership guard)
 * @param userId    caller's user id (recorded in the inventory ledger)
 * @param sellerId  caller's seller profile id — null when the actor is an ADMIN
 *                  (not a seller) or when the actor has no seller profile
 */
public record SkuEditActor(UserRole role, Long userId, Long sellerId) {}
