package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity for a single product attribute row (key-value pair).
 * Enrichment data owned by Radolfa content team — never overwritten by ERP sync.
 *
 * <p>Examples: key="Material" value="Organic Wool", key="Fit" value="Oversized".
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

    @Column(name = "attr_value", nullable = false, length = 512)
    private String attrValue;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
