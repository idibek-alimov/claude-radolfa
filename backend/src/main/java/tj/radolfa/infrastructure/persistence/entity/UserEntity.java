package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import tj.radolfa.domain.model.VehicleType;

import java.math.BigDecimal;

/**
 * JPA persistence model for the {@code users} table.
 *
 * Extends {@link BaseAuditEntity} for optimistic locking ({@code @Version})
 * and standardised {@code created_at}/{@code updated_at} timestamps.
 */
@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone", nullable = false, unique = true, length = 32)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 16)
    private tj.radolfa.domain.model.UserRole role;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "email", unique = true, length = 255)
    private String email;

    @Column(name = "loyalty_points", nullable = false)
    private int loyaltyPoints;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_id")
    private LoyaltyTierEntity tier;

    @Column(name = "spend_to_next_tier")
    private BigDecimal spendToNextTier;

    @Column(name = "spend_to_maintain_tier")
    private BigDecimal spendToMaintainTier;

    @Column(name = "current_month_spending")
    private BigDecimal currentMonthSpending;

    @Column(name = "loyalty_permanent", nullable = false)
    private boolean loyaltyPermanent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lowest_tier_ever_id")
    private LoyaltyTierEntity lowestTierEver;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    // Courier-specific logistics fields
    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", length = 16)
    private VehicleType vehicleType;

    @Column(name = "max_payload_kg", precision = 6, scale = 2)
    private BigDecimal maxPayloadKg;

    @Column(name = "max_length_cm")
    private Integer maxLengthCm;

    @Column(name = "max_width_cm")
    private Integer maxWidthCm;

    @Column(name = "max_height_cm")
    private Integer maxHeightCm;

    // Pickpoint staff assignment (null for non-PICKPOINT_STAFF roles)
    @Column(name = "pickpoint_id")
    private Long pickpointId;

    // Reserved for future zone assignment feature
    @Column(name = "delivery_zone_id")
    private Long deliveryZoneId;
}
