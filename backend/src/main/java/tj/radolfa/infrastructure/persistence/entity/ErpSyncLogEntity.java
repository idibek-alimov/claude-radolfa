package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * JPA persistence model for the {@code erp_sync_log} audit table.
 *
 * Lombok is permitted here – this is infrastructure, not domain.
 */
@Entity
@Table(name = "erp_sync_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErpSyncLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "erp_id", nullable = false, length = 64)
    private String erpId;

    @Column(name = "synced_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Instant syncedAt;

    @Column(name = "status", nullable = false, length = 16)
    private String status;   // "SUCCESS" or "ERROR"

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // ----------------------------------------------------------------
    @PrePersist
    public void prePersist() {
        if (syncedAt == null) syncedAt = Instant.now();
    }
}
