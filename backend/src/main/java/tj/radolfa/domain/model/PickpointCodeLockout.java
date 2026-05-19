package tj.radolfa.domain.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class PickpointCodeLockout {

    private Long    id;
    private final Long    pickpointId;
    private Instant lockedUntil;
    private int     failedCount;

    public PickpointCodeLockout(Long id, Long pickpointId, Instant lockedUntil, int failedCount) {
        this.id           = id;
        this.pickpointId  = pickpointId;
        this.lockedUntil  = lockedUntil;
        this.failedCount  = failedCount;
    }

    public static PickpointCodeLockout newForPickpoint(Long pickpointId) {
        return new PickpointCodeLockout(null, pickpointId, null, 0);
    }

    /** Returns true when the lockout window is currently active. */
    public boolean isActive() {
        return lockedUntil != null && Instant.now().isBefore(lockedUntil);
    }

    /**
     * Increments the failure counter. When the counter reaches {@code threshold},
     * the lockout window is set and the counter is reset for the next window.
     */
    public void recordFailure(int threshold, int lockoutMinutes) {
        this.failedCount++;
        if (this.failedCount >= threshold) {
            this.lockedUntil = Instant.now().plus(lockoutMinutes, ChronoUnit.MINUTES);
            this.failedCount = 0;
        }
    }

    /** Resets the failure counter on a successful verification. */
    public void resetOnSuccess() {
        this.failedCount = 0;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public Long    getId()          { return id; }
    public Long    getPickpointId() { return pickpointId; }
    public Instant getLockedUntil() { return lockedUntil; }
    public int     getFailedCount() { return failedCount; }

    /** Called by the JPA adapter to set the generated ID after the first save. */
    public void setId(Long id)      { this.id = id; }
}
