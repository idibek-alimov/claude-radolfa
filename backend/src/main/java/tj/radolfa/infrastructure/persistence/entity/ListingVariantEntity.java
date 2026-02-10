package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "listing_variants",
       uniqueConstraints = @UniqueConstraint(columnNames = {"product_base_id", "color_key"}))
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class ListingVariantEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_base_id", nullable = false)
    private ProductBaseEntity productBase;

    @Column(name = "color_key", nullable = false, length = 64)
    private String colorKey;

    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    @Column(name = "web_description", columnDefinition = "TEXT")
    private String webDescription;

    @OneToMany(mappedBy = "listingVariant", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ListingVariantImageEntity> images = new ArrayList<>();

    @Column(name = "last_sync_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Instant lastSyncAt;

    @OneToMany(mappedBy = "listingVariant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SkuEntity> skus = new ArrayList<>();
}
