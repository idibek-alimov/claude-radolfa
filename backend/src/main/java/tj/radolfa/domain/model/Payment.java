package tj.radolfa.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable snapshot of a payment transaction.
 *
 * <p>State transitions return new records; nothing mutates in place.
 * This keeps the domain model safe from accidental side effects.
 *
 * <p>Pure Java — zero framework dependencies.
 */
public record Payment(
        Long          id,
        Long          orderId,
        Money         amount,
        String        currency,
        PaymentStatus status,
        String        provider,
        String        providerTransactionId,  // null until provider confirms
        Instant       createdAt,
        Instant       completedAt             // null until terminal state
) {

    public Payment {
        Objects.requireNonNull(orderId,  "orderId must not be null");
        Objects.requireNonNull(amount,   "amount must not be null");
        Objects.requireNonNull(status,   "status must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }
    }

    // ── Factories ─────────────────────────────────────────────────────────────

    /** Creates a new PENDING payment. */
    public static Payment initiate(Long orderId, Money amount, String currency, String provider) {
        return new Payment(null, orderId, amount, currency,
                PaymentStatus.PENDING, provider, null, Instant.now(), null);
    }

    // ── State transitions ─────────────────────────────────────────────────────

    /** Provider acknowledged the request; transaction in flight. */
    public Payment processing(String providerTransactionId) {
        return new Payment(id, orderId, amount, currency,
                PaymentStatus.PROCESSING, provider, providerTransactionId, createdAt, null);
    }

    /** Provider confirmed successful charge. */
    public Payment completed(String providerTransactionId) {
        return new Payment(id, orderId, amount, currency,
                PaymentStatus.COMPLETED, provider, providerTransactionId, createdAt, Instant.now());
    }

    /** Provider reported a failure or timeout. */
    public Payment failed() {
        return new Payment(id, orderId, amount, currency,
                PaymentStatus.FAILED, provider, providerTransactionId, createdAt, Instant.now());
    }

    /** Full or partial refund was issued. */
    public Payment refunded() {
        return new Payment(id, orderId, amount, currency,
                PaymentStatus.REFUNDED, provider, providerTransactionId, createdAt, Instant.now());
    }

    /** Payment was cancelled before completion. */
    public Payment cancelled() {
        return new Payment(id, orderId, amount, currency,
                PaymentStatus.CANCELLED, provider, providerTransactionId, createdAt, Instant.now());
    }
}
