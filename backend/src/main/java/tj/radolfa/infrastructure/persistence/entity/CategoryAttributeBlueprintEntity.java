package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import tj.radolfa.domain.model.AttributeType;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "category_attribute_blueprints",
    uniqueConstraints = @UniqueConstraint(columnNames = {"category_id", "attribute_key"})
)
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAttributeBlueprintEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryEntity category;

    @Column(name = "attribute_key", nullable = false, length = 128)
    private String attributeKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AttributeType type;

    @Column(name = "unit_name", length = 64)
    private String unitName;

    @Column(name = "is_required", nullable = false)
    private boolean required;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @OneToMany(mappedBy = "blueprint", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @BatchSize(size = 50)
    private List<CategoryAttributeBlueprintValueEntity> allowedValues = new ArrayList<>();
}
