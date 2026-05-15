package tj.radolfa.domain.model;

import java.time.Instant;

/**
 * A one-time verification code sent to the customer when an order is ready for handoff.
 *
 * <p>For home delivery: generated when order moves to {@code SHIPPED}.
 * For pickpoint: generated when order moves to {@code READY_FOR_PICKUP}.
 * The courier or pickpoint staff enters this code to confirm handoff, transitioning the order to DELIVERED.
 *
 * <p>Pure Java — zero Spring / JPA / Jackson dependencies.
 */
public class DeliveryCode {

    private final Long    id;
    private final Long    orderId;
    private final String  code;
    private final Instant expiresAt;
    private final Instant createdAt;

    private Instant usedAt;
    private int     attemptCount;

    public DeliveryCode(Long id, Long orderId, String code,
                        Instant expiresAt, Instant usedAt,
                        int attemptCount, Instant createdAt) {
        this.id           = id;
        this.orderId      = orderId;
        this.code         = code;
        this.expiresAt    = expiresAt;
        this.usedAt       = usedAt;
        this.attemptCount = attemptCount;
        this.createdAt    = createdAt;
    }

    /** Marks this code as successfully used. */
    public void markUsed() {
        this.usedAt = Instant.now();
    }

    /** Increments the failed attempt counter. */
    public void incrementAttempts() {
        this.attemptCount++;
    }

    /** Returns true if the code's expiry time has passed. */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /** Returns true if this code has already been used to confirm a delivery. */
    public boolean isUsed() {
        return usedAt != null;
    }

    // ---- Getters ----
    public Long    getId()           { return id; }
    public Long    getOrderId()      { return orderId; }
    public String  getCode()         { return code; }
    public Instant getExpiresAt()    { return expiresAt; }
    public Instant getUsedAt()       { return usedAt; }
    public int     getAttemptCount() { return attemptCount; }
    public Instant getCreatedAt()    { return createdAt; }
}
