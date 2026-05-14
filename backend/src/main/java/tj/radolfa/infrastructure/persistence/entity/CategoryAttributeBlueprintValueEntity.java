package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "category_attribute_blueprint_values")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAttributeBlueprintValueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blueprint_id", nullable = false)
    private CategoryAttributeBlueprintEntity blueprint;

    @Column(name = "allowed_value", nullable = false, length = 256)
    private String allowedValue;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
