package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "listing_variants", uniqueConstraints = @UniqueConstraint(columnNames = { "product_base_id",
        "color_id" }))
@BatchSize(size = 50)
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_id", nullable = false)
    private ColorEntity color;

    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    @Column(name = "product_code", unique = true, length = 10)
    private String productCode;

    @Column(name = "web_description", columnDefinition = "TEXT")
    private String webDescription;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "listing_variant_tags",
            joinColumns = @JoinColumn(name = "variant_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @BatchSize(size = 50)
    private Set<ProductTagEntity> tags = new HashSet<>();

    @Column(name = "weight_kg")
    private Double weightKg;

    @Column(name = "width_cm")
    private Integer widthCm;

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "depth_cm")
    private Integer depthCm;

    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled = false;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @OneToMany(mappedBy = "listingVariant", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @BatchSize(size = 50)
    private List<ListingVariantImageEntity> images = new ArrayList<>();

    @OneToMany(mappedBy = "listingVariant", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @BatchSize(size = 50)
    private List<ListingVariantAttributeEntity> attributes = new ArrayList<>();

    @Column(name = "last_sync_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Instant lastSyncAt;

    @OneToMany(mappedBy = "listingVariant", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<SkuEntity> skus = new ArrayList<>();
}
