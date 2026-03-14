package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "loyalty_tiers")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyTierEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "discount_percentage", nullable = false)
    private BigDecimal discountPercentage;

    @Column(name = "cashback_percentage", nullable = false)
    private BigDecimal cashbackPercentage;

    @Column(name = "min_spend_requirement", nullable = false)
    private BigDecimal minSpendRequirement;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "color", nullable = false, length = 7)
    private String color;
}
