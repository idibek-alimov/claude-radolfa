package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tj.radolfa.domain.model.OrderStatus;

import java.time.Instant;

@Entity
@Table(name = "order_status_changes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusChangeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_from", length = 32)
    private OrderStatus statusFrom;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_to", nullable = false, length = 32)
    private OrderStatus statusTo;

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
