package tj.radolfa.domain.exception;

public class BinWarehouseMismatchException extends RuntimeException {

    private final Long binId;
    private final Long warehouseId;

    public BinWarehouseMismatchException(Long binId, Long warehouseId) {
        super("Bin id=" + binId + " does not belong to warehouse id=" + warehouseId);
        this.binId       = binId;
        this.warehouseId = warehouseId;
    }

    public Long getBinId()       { return binId; }
    public Long getWarehouseId() { return warehouseId; }
}
