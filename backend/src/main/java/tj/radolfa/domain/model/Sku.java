package tj.radolfa.domain.model;

/**
 * A size/price variant — the actual purchasable unit.
 *
 * <p>All pricing and stock fields are authoritative-source-locked and
 * <b>always</b> overwritten on every sync via {@link #updatePriceAndStock}.
 *
 * <p>Logistics fields (barcode, weight, dimensions) are Radolfa-managed only
 * and are never overwritten by ERP sync.
 *
 * <p>Pure Java — zero Spring / JPA / Jackson / Lombok dependencies.
 */
public class Sku {

    private final Long   id;
    private final Long   listingVariantId;
    private final String skuCode;

    private String  sizeLabel;

    // Authoritative-source-locked fields — always overwritten on sync
    private Integer stockQuantity;
    private Money   price;

    // Radolfa-managed logistics fields — never touched by ERP sync
    private String  barcode;

    public Sku(Long id,
               Long listingVariantId,
               String skuCode,
               String sizeLabel,
               Integer stockQuantity,
               Money price,
               String barcode) {
        this.id               = id;
        this.listingVariantId = listingVariantId;
        this.skuCode          = skuCode;
        this.sizeLabel        = sizeLabel;
        this.stockQuantity    = stockQuantity;
        this.price            = price;
        this.barcode          = barcode;
    }

    /** Constructor used by ERP sync path (no barcode). */
    public Sku(Long id,
               Long listingVariantId,
               String skuCode,
               String sizeLabel,
               Integer stockQuantity,
               Money price) {
        this(id, listingVariantId, skuCode, sizeLabel, stockQuantity, price, null);
    }

    /**
     * Authoritative-source merge — overwrites ALL pricing and stock fields.
     * This is the single authorised write path for ERP sync.
     */
    public void updatePriceAndStock(Money price, Integer stockQuantity) {
        this.price         = price;
        this.stockQuantity = stockQuantity;
    }

    /**
     * Updates the size label. Called when the external source sends a different label.
     */
    public void updateSizeLabel(String sizeLabel) {
        this.sizeLabel = sizeLabel;
    }

    // ---- Getters ----
    public Long    getId()               { return id; }
    public Long    getListingVariantId() { return listingVariantId; }
    public String  getSkuCode()          { return skuCode; }
    public String  getSizeLabel()        { return sizeLabel; }
    public Integer getStockQuantity()    { return stockQuantity; }
    public Money   getPrice()            { return price; }
    public String  getBarcode()          { return barcode; }
}
