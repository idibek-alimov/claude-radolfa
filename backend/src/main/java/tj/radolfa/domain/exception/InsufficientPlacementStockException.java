package tj.radolfa.domain.exception;

public class InsufficientPlacementStockException extends RuntimeException {

    private final Long skuId;
    private final Long sourceBinId;   // null = inbound pool
    private final int  available;
    private final int  requested;

    public InsufficientPlacementStockException(Long skuId, Long sourceBinId, int available, int requested) {
        super("Insufficient placement stock for SKU id=" + skuId
              + " source=" + (sourceBinId == null ? "inbound" : "bin:" + sourceBinId)
              + " (available=" + available + ", requested=" + requested + ")");
        this.skuId      = skuId;
        this.sourceBinId = sourceBinId;
        this.available  = available;
        this.requested  = requested;
    }

    public Long getSkuId()       { return skuId; }
    public Long getSourceBinId() { return sourceBinId; }
    public int  getAvailable()   { return available; }
    public int  getRequested()   { return requested; }
}
