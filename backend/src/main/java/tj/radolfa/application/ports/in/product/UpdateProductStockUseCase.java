package tj.radolfa.application.ports.in.product;

/**
 * In-Port: adjust or set the stock quantity for a SKU.
 *
 * <p>Two modes:
 * <ul>
 *   <li>{@link #setAbsolute} — used by ADMIN or an owning SELLER for manual stock correction.</li>
 *   <li>{@link #adjust} — used by ADMIN or an owning SELLER for signed delta adjustments.</li>
 * </ul>
 *
 * <p>The service enforces ownership: ADMIN may edit any SKU; a SELLER may edit only SKUs of
 * products they own; Radolfa-owned products are ADMIN-only. Violations throw
 * {@code FieldLockException} (mapped to 403).
 *
 * <p>Internal system paths (checkout, cancellation, stock receipts) go through
 * {@code StockAdjustmentPort} directly — those paths bypass the guard intentionally.
 */
public interface UpdateProductStockUseCase {

    /**
     * Sets stock to an exact value.
     *
     * @param skuId    target SKU
     * @param quantity must be ≥ 0
     * @param actor    the caller's identity (role, userId, sellerId)
     */
    void setAbsolute(Long skuId, int quantity, SkuEditActor actor);

    /**
     * Adjusts stock by a signed delta.
     * Negative delta = decrement; positive = increment.
     *
     * @param actor the caller's identity (role, userId, sellerId)
     * @throws IllegalStateException if the resulting stock would go below 0
     */
    void adjust(Long skuId, int delta, SkuEditActor actor);
}
