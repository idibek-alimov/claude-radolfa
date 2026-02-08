package tj.radolfa.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value object representing a monetary amount.
 *
 * <p>Pure Java — zero framework dependencies. Immutable.
 *
 * <p>Invariants:
 * <ul>
 *   <li>{@code amount} is never null.</li>
 *   <li>{@code amount} is never negative.</li>
 * </ul>
 *
 * <p>Nullable fields (e.g. {@code Product.price} before first ERP sync)
 * are modelled as {@code Money price = null}, NOT {@code Money.of(null)}.
 */
public record Money(BigDecimal amount) {

    public Money {
        Objects.requireNonNull(amount, "Money amount cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Money amount cannot be negative: " + amount);
        }
    }

    /** Shared zero instance — avoids repeated allocation. */
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    /**
     * Factory method. Returns {@code null} when the input is null,
     * a valid {@code Money} otherwise.
     */
    public static Money of(BigDecimal amount) {
        return amount != null ? new Money(amount) : null;
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money multiply(int quantity) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)));
    }
}
