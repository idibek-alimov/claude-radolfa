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
import tj.radolfa.domain.model.LoyaltyReason;

import java.time.Instant;

@Entity
@Table(name = "loyalty_ledger")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyLedgerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "delta", nullable = false)
    private int delta;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 24)
    private LoyaltyReason reason;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "note", length = 255)
    private String note;

    @Column(name = "source_lot_id")
    private Long sourceLotId;

    @Column(name = "remaining_points")
    private Integer remainingPoints;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "balance_after", nullable = false)
    private int balanceAfter;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onPrePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
