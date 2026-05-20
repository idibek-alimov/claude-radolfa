package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "warehouse_bins",
       uniqueConstraints = @UniqueConstraint(columnNames = {"shelf_id", "code"}))
@Data
@EqualsAndHashCode(callSuper = false, exclude = {"shelf"})
@ToString(exclude = {"shelf"})
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseBinEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shelf_id", nullable = false)
    private WarehouseShelfEntity shelf;

    @Column(name = "code", nullable = false, length = 20)
    private String code;
}
