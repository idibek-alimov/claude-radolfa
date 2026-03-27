package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for a single product attribute row (key + ordered list of values).
 * Enrichment data owned by Radolfa content team — never overwritten by ERP sync.
 *
 * <p>Examples: key="Material" values=["Cotton","Acrylic"], key="Fit" values=["Oversized"].
 */
@Entity
@Table(name = "listing_variant_attributes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListingVariantAttributeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_variant_id", nullable = false)
    private ListingVariantEntity listingVariant;

    @Column(name = "attr_key", nullable = false, length = 128)
    private String attrKey;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @OneToMany(mappedBy = "attribute", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @BatchSize(size = 50)
    private List<ListingVariantAttributeValueEntity> values = new ArrayList<>();
}
