package tj.radolfa.application.ports.out;

public interface AtomicStockPort {

    /**
     * Atomically decrements stock only when current stock >= qty.
     * Returns 1 if the row was updated, 0 if stock was insufficient or SKU not found.
     */
    int decrementIfAvailable(Long skuId, int qty);

    /**
     * Atomically increments stock.
     * Returns 1 if the row was updated, 0 if the SKU was not found.
     */
    int increment(Long skuId, int qty);
}
