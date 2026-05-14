package tj.radolfa.domain.exception;

public class InsufficientStockException extends RuntimeException {

    private final Long skuId;
    private final int available;
    private final int requested;

    public InsufficientStockException(Long skuId, int available, int requested) {
        super("Insufficient stock for SKU id=" + skuId
              + " (available=" + available + ", requested=" + requested + ")");
        this.skuId     = skuId;
        this.available = available;
        this.requested = requested;
    }

    public Long getSkuId()    { return skuId; }
    public int getAvailable() { return available; }
    public int getRequested() { return requested; }
}
