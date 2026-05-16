package tj.radolfa.domain.model;

import java.math.BigDecimal;

/**
 * A size/price variant — the actual purchasable unit.
 *
 * <p>Pricing and stock fields are ADMIN-managed and must only be written
 * via {@link #updatePriceAndStock}.
 *
 * <p>Logistics fields (barcode, weight, dimensions) are Radolfa-managed only.
 *
 * <p>Pure Java — zero Spring / JPA / Jackson / Lombok dependencies.
 */
public class Sku {

    private final Long   id;
    private final Long   listingVariantId;
    private final String skuCode;

    private String  sizeLabel;

    // ADMIN-managed fields
    private Integer stockQuantity;
    private Money   price;

    // Radolfa-managed logistics fields
    private String     barcode;
    private BigDecimal weightKg;
    private Integer    lengthCm;
    private Integer    widthCm;
    private Integer    heightCm;

    public Sku(Long id,
               Long listingVariantId,
               String skuCode,
               String sizeLabel,
               Integer stockQuantity,
               Money price,
               String barcode,
               BigDecimal weightKg,
               Integer lengthCm,
               Integer widthCm,
               Integer heightCm) {
        this.id               = id;
        this.listingVariantId = listingVariantId;
        this.skuCode          = skuCode;
        this.sizeLabel        = sizeLabel;
        this.stockQuantity    = stockQuantity;
        this.price            = price;
        this.barcode          = barcode;
        this.weightKg         = weightKg;
        this.lengthCm         = lengthCm;
        this.widthCm          = widthCm;
        this.heightCm         = heightCm;
    }

    /** Constructor without logistics fields — preserves backward compatibility. */
    public Sku(Long id,
               Long listingVariantId,
               String skuCode,
               String sizeLabel,
               Integer stockQuantity,
               Money price,
               String barcode) {
        this(id, listingVariantId, skuCode, sizeLabel, stockQuantity, price, barcode,
             null, null, null, null);
    }

    /** Constructor without barcode or logistics fields. */
    public Sku(Long id,
               Long listingVariantId,
               String skuCode,
               String sizeLabel,
               Integer stockQuantity,
               Money price) {
        this(id, listingVariantId, skuCode, sizeLabel, stockQuantity, price, null,
             null, null, null, null);
    }

    /**
     * ADMIN-only write path — overwrites ALL pricing and stock fields.
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

    /**
     * MANAGER/ADMIN write path for logistics dimensions.
     * Any null value clears that field.
     */
    public void updateLogistics(BigDecimal weightKg, Integer lengthCm, Integer widthCm, Integer heightCm) {
        this.weightKg = weightKg;
        this.lengthCm = lengthCm;
        this.widthCm  = widthCm;
        this.heightCm = heightCm;
    }

    // ---- Getters ----
    public Long       getId()               { return id; }
    public Long       getListingVariantId() { return listingVariantId; }
    public String     getSkuCode()          { return skuCode; }
    public String     getSizeLabel()        { return sizeLabel; }
    public Integer    getStockQuantity()    { return stockQuantity; }
    public Money      getPrice()            { return price; }
    public String     getBarcode()          { return barcode; }
    public BigDecimal getWeightKg()         { return weightKg; }
    public Integer    getLengthCm()         { return lengthCm; }
    public Integer    getWidthCm()          { return widthCm; }
    public Integer    getHeightCm()         { return heightCm; }
}
