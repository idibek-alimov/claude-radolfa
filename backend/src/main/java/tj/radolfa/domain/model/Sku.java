package tj.radolfa.domain.model;

/**
 * A size/price variant — the actual purchasable unit.
 *
 * <p>All pricing and stock fields are authoritative-source-locked and
 * <b>always</b> overwritten on every sync via {@link #updatePriceAndStock}.
 *
 * <p>Pure Java — zero Spring / JPA / Jackson / Lombok dependencies.
 */
public class Sku {

    private final Long   id;
    private final Long   listingVariantId;
    private final String skuCode;

    private String  sizeLabel;

    // Authoritative-source-locked fields — always overwritten
    private Integer stockQuantity;
    private Money   price;              // Original / list price

    public Sku(Long id,
               Long listingVariantId,
               String skuCode,
               String sizeLabel,
               Integer stockQuantity,
               Money price) {
        this.id               = id;
        this.listingVariantId = listingVariantId;
        this.skuCode          = skuCode;
        this.sizeLabel        = sizeLabel;
        this.stockQuantity    = stockQuantity;
        this.price            = price;
    }

    /**
     * Authoritative-source merge — overwrites ALL pricing and stock fields.
     * This is the single authorised write path.
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
    public Long    getId()              { return id; }
    public Long    getListingVariantId() { return listingVariantId; }
    public String  getSkuCode()         { return skuCode; }
    public String  getSizeLabel()       { return sizeLabel; }
    public Integer getStockQuantity()   { return stockQuantity; }
    public Money   getPrice()           { return price; }
}
