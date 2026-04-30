package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import tj.radolfa.domain.model.ReviewTraitInputType;

@Entity
@Table(name = "review_trait")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTraitEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trait_key", nullable = false, unique = true, length = 64)
    private String traitKey;

    @Column(name = "label_i18n", nullable = false)
    private String labelI18n;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false, length = 16)
    private ReviewTraitInputType inputType;
}
