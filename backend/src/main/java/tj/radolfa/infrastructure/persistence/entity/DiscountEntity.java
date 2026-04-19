package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "discounts")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class DiscountEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "discount_type_id", nullable = false)
    private DiscountTypeEntity type;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "discount_items",
            joinColumns = @JoinColumn(name = "discount_id")
    )
    @Column(name = "item_code", length = 64)
    private List<String> itemCodes = new ArrayList<>();

    @Column(name = "amount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountValue;

    @Column(name = "amount_type", nullable = false, length = 16)
    private String amountType;

    @Column(name = "valid_from", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Instant validFrom;

    @Column(name = "valid_upto", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Instant validUpto;

    @Column(name = "is_disabled", nullable = false)
    private boolean disabled;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @Column(name = "min_basket_amount", precision = 12, scale = 2)
    private BigDecimal minBasketAmount;

    @Column(name = "usage_cap_total")
    private Integer usageCapTotal;

    @Column(name = "usage_cap_per_customer")
    private Integer usageCapPerCustomer;

    @Column(name = "coupon_code", length = 64)
    private String couponCode;

    @OneToMany(mappedBy = "discount", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DiscountTargetEntity> targets = new ArrayList<>();
}
