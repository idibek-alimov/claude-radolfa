package tj.radolfa.domain.model;

import java.math.BigDecimal;

/**
 * A single line item in a {@link Cart}.
 *
 * <p>Pure Java — zero Spring / JPA / Jackson / Lombok dependencies.
 *
 * <p>The {@code priceSnapshot} is captured at the moment the item is added to
 * the cart from ERP data. It is NOT updated retroactively when ERP prices change,
 * preserving the price the user saw when they added the item.
 */
public class CartItem {

    private final Long skuId;
    private final String listingSlug;
    private final String productName;
    private final String sizeLabel;
    private final String imageUrl;
    private final BigDecimal priceSnapshot;
    private int quantity;

    public CartItem(Long skuId,
                    String listingSlug,
                    String productName,
                    String sizeLabel,
                    String imageUrl,
                    BigDecimal priceSnapshot,
                    int quantity) {
        if (skuId == null) {
            throw new IllegalArgumentException("CartItem skuId must not be null");
        }
        if (priceSnapshot == null || priceSnapshot.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("CartItem priceSnapshot must be non-negative");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("CartItem quantity must be positive, got: " + quantity);
        }
        this.skuId = skuId;
        this.listingSlug = listingSlug;
        this.productName = productName;
        this.sizeLabel = sizeLabel;
        this.imageUrl = imageUrl;
        this.priceSnapshot = priceSnapshot;
        this.quantity = quantity;
    }

    /**
     * Returns the subtotal for this line item: priceSnapshot * quantity.
     */
    public Money itemSubtotal() {
        return Money.of(priceSnapshot).multiply(quantity);
    }

    // ---- Getters ----
    public Long       getSkuId()         { return skuId; }
    public String     getListingSlug()   { return listingSlug; }
    public String     getProductName()   { return productName; }
    public String     getSizeLabel()     { return sizeLabel; }
    public String     getImageUrl()      { return imageUrl; }
    public BigDecimal getPriceSnapshot() { return priceSnapshot; }
    public int        getQuantity()      { return quantity; }

    public void setQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("CartItem quantity must be positive, got: " + quantity);
        }
        this.quantity = quantity;
    }
}
