package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id", foreignKey = @ForeignKey(name = "fk_order_item_sku"))
    private SkuEntity sku;

    @Column(name = "sku_code", length = 128)
    private String skuCode;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    /** Final unit price actually charged to the customer (post-discount). */
    @Column(name = "price_at_purchase", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAtPurchase;

    /** Pre-discount base unit price (the cart's add-to-cart snapshot). Null for items predating this audit trail. */
    @Column(name = "original_unit_price", precision = 12, scale = 2)
    private BigDecimal originalUnitPrice;

    /** Which pricing mechanism produced {@code priceAtPurchase}: NONE | CAMPAIGN | LOYALTY. */
    @Column(name = "discount_mechanism", length = 16)
    private String discountMechanism;

    @Column(name = "effective_discount_percent", precision = 5, scale = 2)
    private BigDecimal effectiveDiscountPercent;

    @Column(name = "loyalty_tier_percent", precision = 5, scale = 2)
    private BigDecimal loyaltyTierPercent;

    @Column(name = "quantity_picked", nullable = false)
    private int quantityPicked;

    @Column(name = "picked_at")
    private Instant pickedAt;

    @Column(name = "picked_by_user_id")
    private Long pickedByUserId;

    /** Snapshot of {@code sellers.id} at checkout. {@code null} = Radolfa-owned item. */
    @Column(name = "seller_id")
    private Long sellerId;
}
