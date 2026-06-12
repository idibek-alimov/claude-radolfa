package tj.radolfa.application.ports.in.product;

import tj.radolfa.domain.model.Money;

/**
 * In-Port: set the price on a specific SKU.
 *
 * <p>The service enforces ownership: ADMIN may edit any SKU; a SELLER may edit
 * only SKUs of products they own; Radolfa-owned products are ADMIN-only.
 * Violations throw {@code FieldLockException} (mapped to 403 by the exception handler).
 */
public interface UpdateProductPriceUseCase {

    /**
     * @param skuId    the ID of the SKU to update
     * @param newPrice must be non-null and non-negative
     * @param actor    the caller's identity (role, userId, sellerId); sellerId is null for ADMINs
     */
    void execute(Long skuId, Money newPrice, SkuEditActor actor);
}
