package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "pickpoint_code_lockouts")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class PickpointCodeLockoutEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pickpoint_id", nullable = false, unique = true)
    private Long pickpointId;

    @Column(name = "locked_until")
    @Temporal(TemporalType.TIMESTAMP)
    private Instant lockedUntil;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;
}
