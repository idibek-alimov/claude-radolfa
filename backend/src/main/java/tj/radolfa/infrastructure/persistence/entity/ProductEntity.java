package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA persistence model for the {@code products} table.
 *
 * Extends {@link BaseAuditEntity} for optimistic locking ({@code @Version})
 * and standardised {@code created_at}/{@code updated_at} timestamps.
 *
 * {@code @SQLRestriction} hides soft-deleted rows from all standard queries.
 */
@Entity
@Table(name = "products")
@SQLRestriction("deleted_at IS NULL")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class ProductEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "erp_id", nullable = false, unique = true, length = 64)
    private String erpId;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "stock")
    private Integer stock;

    @Column(name = "web_description", columnDefinition = "TEXT")
    private String webDescription;

    @Column(name = "is_top_selling", nullable = false)
    private boolean topSelling;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProductImageEntity> images = new ArrayList<>();

    @Column(name = "last_erp_sync_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Instant lastErpSyncAt;

    @Column(name = "deleted_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Instant deletedAt;
}
