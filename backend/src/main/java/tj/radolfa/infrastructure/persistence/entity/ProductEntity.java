package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * JPA persistence model for the {@code products} table.
 *
 * Lombok {@link Data} is fine here – this is infrastructure, not domain.
 * The {@code images} column is a native Postgres TEXT[] array; Hibernate
 * handles it transparently via {@link ElementCollection} with a
 * {@link CollectionTable} pointing at a single-column layout.  However,
 * because the DDL uses a plain array column (not a join table), we use
 * a custom {@link StringArrayConverter} instead.
 */
@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductEntity {

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

    @Convert(converter = StringArrayConverter.class)
    @Column(name = "images", columnDefinition = "TEXT[]")
    private List<String> images;

    @Column(name = "last_erp_sync_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Instant lastErpSyncAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Instant updatedAt;

    // ----------------------------------------------------------------
    // Lifecycle hooks
    // ----------------------------------------------------------------
    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
