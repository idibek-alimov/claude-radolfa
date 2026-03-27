package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "listing_variant_attribute_values")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListingVariantAttributeValueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id", nullable = false)
    private ListingVariantAttributeEntity attribute;

    @Column(name = "value", nullable = false, length = 512)
    private String value;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
