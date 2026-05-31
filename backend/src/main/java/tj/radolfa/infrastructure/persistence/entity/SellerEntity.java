package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * JPA persistence model for the {@code sellers} table.
 *
 * Extends {@link BaseAuditEntity} for optimistic locking ({@code @Version})
 * and standardised {@code created_at}/{@code updated_at} timestamps.
 */
@Entity
@Table(name = "sellers")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class SellerEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "shop_name", nullable = false, length = 120)
    private String shopName;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "bio")
    private String bio;
}
