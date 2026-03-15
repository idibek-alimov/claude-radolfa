package tj.radolfa.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A size/price variant — the actual purchasable unit (one ERPNext Item).
 *
 * <p>All pricing and stock fields are ERP-locked and <b>always</b>
 * overwritten on every sync via {@link #updateFromErp}.
 *
 * <p>Pure Java — zero Spring / JPA / Jackson / Lombok dependencies.
 */
public class Sku {

    private final Long   id;
    private final Long   listingVariantId;
    private final String erpItemCode;

    private String  sizeLabel;

    // ERP-locked fields — always overwritten
    private Integer    stockQuantity;
    private Money      price;              // Original / list price
    private Money      salePrice;          // Effective price (after promotions)
    private Instant    saleEndsAt;
    private BigDecimal discountPercentage; // ERP product discount % (source of truth)

    public Sku(Long id,
               Long listingVariantId,
               String erpItemCode,
               String sizeLabel,
               Integer stockQuantity,
               Money price,
               Money salePrice,
               Instant saleEndsAt,
               BigDecimal discountPercentage) {
        this.id                 = id;
        this.listingVariantId   = listingVariantId;
        this.erpItemCode        = erpItemCode;
        this.sizeLabel          = sizeLabel;
        this.stockQuantity      = stockQuantity;
        this.price              = price;
        this.salePrice          = salePrice;
        this.saleEndsAt         = saleEndsAt;
        this.discountPercentage = discountPercentage;
    }

    /**
     * ERP merge — overwrites ALL pricing and stock fields.
     * This is the single authorised write path.
     */
    public void updateFromErp(Integer stockQuantity,
                              Money price,
                              Money salePrice,
                              Instant saleEndsAt,
                              BigDecimal discountPercentage) {
        this.stockQuantity      = stockQuantity;
        this.price              = price;
        this.salePrice          = salePrice;
        this.saleEndsAt         = saleEndsAt;
        this.discountPercentage = discountPercentage;
    }

    /**
     * Updates the size label. Called when ERP sends a different label.
     */
    public void updateSizeLabel(String sizeLabel) {
        this.sizeLabel = sizeLabel;
    }

    // ---- Queries ----

    public boolean isOnSale() {
        if (salePrice == null || price == null) return false;
        if (saleEndsAt != null && Instant.now().isAfter(saleEndsAt)) return false;
        return salePrice.amount().compareTo(price.amount()) < 0;
    }

    // ---- Getters ----
    public Long    getId()               { return id; }
    public Long    getListingVariantId()  { return listingVariantId; }
    public String  getErpItemCode()      { return erpItemCode; }
    public String  getSizeLabel()        { return sizeLabel; }
    public Integer getStockQuantity()    { return stockQuantity; }
    public Money   getPrice()            { return price; }
    public Money   getSalePrice()        { return salePrice; }
    public Instant    getSaleEndsAt()         { return saleEndsAt; }
    public BigDecimal getDiscountPercentage() { return discountPercentage; }
}
