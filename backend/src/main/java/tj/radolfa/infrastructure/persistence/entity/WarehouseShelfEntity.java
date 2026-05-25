package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "warehouse_shelves",
       uniqueConstraints = @UniqueConstraint(columnNames = {"zone_id", "code"}))
@Data
@EqualsAndHashCode(callSuper = false, exclude = {"zone", "bins"})
@ToString(exclude = {"zone", "bins"})
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseShelfEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private WarehouseZoneEntity zone;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "label", length = 100)
    private String label;

    @OneToMany(mappedBy = "shelf", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.EAGER)
    private List<WarehouseBinEntity> bins = new ArrayList<>();
}
