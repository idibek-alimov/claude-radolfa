package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Mapped superclass providing optimistic locking and audit timestamps
 * to all core JPA entities.
 *
 * <ul>
 *   <li>{@code version} — Hibernate {@code @Version} for optimistic locking.
 *       Concurrent stale writes throw {@link jakarta.persistence.OptimisticLockException}.</li>
 *   <li>{@code createdAt} — Set once on first persist, never updated.</li>
 *   <li>{@code updatedAt} — Refreshed on every persist/update.</li>
 * </ul>
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseAuditEntity {

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Instant updatedAt;

    @PrePersist
    public void onPrePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    public void onPreUpdate() {
        updatedAt = Instant.now();
    }
}
