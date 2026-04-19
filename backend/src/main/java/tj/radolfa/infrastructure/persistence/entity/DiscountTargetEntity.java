package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "discount_target")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscountTargetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "discount_id", nullable = false)
    private DiscountEntity discount;

    @Column(name = "target_type", nullable = false, length = 16)
    private String targetType;

    @Column(name = "reference_id", nullable = false, length = 128)
    private String referenceId;

    @Column(name = "include_descendants", nullable = false)
    private boolean includeDescendants;
}
