package tj.radolfa.domain.exception;

public class BarcodeMismatchException extends RuntimeException {

    private final String scannedBarcode;
    private final Long orderId;

    public BarcodeMismatchException(String scannedBarcode, Long orderId) {
        super("Scanned barcode '" + scannedBarcode + "' does not match any unpicked item on order " + orderId);
        this.scannedBarcode = scannedBarcode;
        this.orderId = orderId;
    }

    public String getScannedBarcode() { return scannedBarcode; }
    public Long getOrderId() { return orderId; }
}
