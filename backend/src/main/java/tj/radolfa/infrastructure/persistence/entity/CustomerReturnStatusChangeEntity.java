package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tj.radolfa.domain.model.CustomerReturnStatus;

import java.time.Instant;

@Entity
@Table(name = "customer_return_status_changes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerReturnStatusChangeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "return_id", nullable = false)
    private Long returnId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_from", length = 32)
    private CustomerReturnStatus statusFrom;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_to", nullable = false, length = 32)
    private CustomerReturnStatus statusTo;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @PrePersist
    void onPrePersist() {
        if (occurredAt == null) occurredAt = Instant.now();
    }
}
