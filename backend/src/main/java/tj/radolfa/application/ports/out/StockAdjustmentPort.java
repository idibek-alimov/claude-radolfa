package tj.radolfa.application.ports.out;

/**
 * Out-Port: adjust or set stock quantities on the persistence layer.
 *
 * <p>Separate from {@code SaveProductHierarchyPort} to allow lightweight
 * stock updates (e.g. after checkout) without loading the full hierarchy.
 */
public interface StockAdjustmentPort {

    /**
     * Decrements stock for a SKU.
     *
     * @param skuId    target SKU
     * @param quantity must be > 0
     * @throws IllegalStateException if the resulting stock would go below 0
     */
    void decrement(Long skuId, int quantity);

    /**
     * Increments stock for a SKU (e.g. after order cancellation or restock).
     *
     * @param skuId    target SKU
     * @param quantity must be > 0
     */
    void increment(Long skuId, int quantity);

    /**
     * Sets stock to an absolute value.
     * Used by ADMIN for manual corrections.
     *
     * @param skuId    target SKU
     * @param quantity must be ≥ 0
     */
    void setAbsolute(Long skuId, int quantity);
}
