package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_bases")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class ProductBaseEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "erp_template_code", nullable = false, unique = true, length = 64)
    private String erpTemplateCode;

    @Column(name = "name", length = 255)
    private String name;

    @OneToMany(mappedBy = "productBase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ListingVariantEntity> variants = new ArrayList<>();
}
