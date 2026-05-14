package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "discount_application")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscountApplicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "discount_id", nullable = false)
    private DiscountEntity discount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(name = "order_line_id", nullable = false)
    private Long orderLineId;

    @Column(name = "sku_item_code", nullable = false, length = 128)
    private String skuItemCode;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "original_unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal originalUnitPrice;

    @Column(name = "applied_unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal appliedUnitPrice;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;
}
