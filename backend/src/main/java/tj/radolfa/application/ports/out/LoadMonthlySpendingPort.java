package tj.radolfa.application.ports.out;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Out-Port: compute a user's net spending within a time window.
 *
 * <p>Net spending = sum of COMPLETED payment amounts − sum of REFUNDED payment amounts,
 * where {@code completedAt} falls in [{@code from}, {@code to}).
 */
public interface LoadMonthlySpendingPort {

    BigDecimal calculateNetSpending(Long userId, Instant from, Instant to);
}
