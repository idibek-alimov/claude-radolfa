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

    @Column(name = "erp_pricing_rule_id", nullable = false, unique = true, length = 140)
    private String erpPricingRuleId;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "discount_items",
            joinColumns = @JoinColumn(name = "discount_id")
    )
    @Column(name = "item_code", length = 64)
    private List<String> itemCodes = new ArrayList<>();

    @Column(name = "discount_value", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountValue;

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
}
