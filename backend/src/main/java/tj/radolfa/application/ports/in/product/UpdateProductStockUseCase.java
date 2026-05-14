package tj.radolfa.application.ports.in.product;

/**
 * In-Port: adjust or set the stock quantity for a SKU.
 *
 * <p>Two modes:
 * <ul>
 *   <li>{@link #setAbsolute} — used by ADMIN for manual stock correction.</li>
 *   <li>{@link #adjust} — used internally after checkout (negative delta)
 *       or cancellation / restock (positive delta).</li>
 * </ul>
 */
public interface UpdateProductStockUseCase {

    /**
     * Sets stock to an exact value. ADMIN use only.
     *
     * @param skuId    target SKU
     * @param quantity must be ≥ 0
     */
    void setAbsolute(Long skuId, int quantity);

    /**
     * Adjusts stock by a signed delta.
     * Negative delta = decrement (e.g. after checkout).
     * Positive delta = increment (e.g. after cancellation/restock).
     *
     * @throws IllegalStateException if the resulting stock would go below 0
     */
    void adjust(Long skuId, int delta);
}
