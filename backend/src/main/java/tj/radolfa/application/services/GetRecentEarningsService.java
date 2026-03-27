package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.service.LoyaltyCalculator;

import java.time.Instant;
import java.util.List;

/**
 * Query service: returns approximate cashback points earned for a user's
 * recent PAID orders, using their current loyalty tier.
 *
 * <p>This is an approximation — the user's tier may have changed since each
 * order was placed, but it is accurate enough for profile display purposes.
 */
@Service
public class GetRecentEarningsService {

    private static final int RECENT_ORDERS_LIMIT = 5;

    private final LoadOrderPort     loadOrderPort;
    private final LoadUserPort      loadUserPort;
    private final LoyaltyCalculator loyaltyCalculator;

    public GetRecentEarningsService(LoadOrderPort loadOrderPort,
                                    LoadUserPort loadUserPort,
                                    LoyaltyCalculator loyaltyCalculator) {
        this.loadOrderPort     = loadOrderPort;
        this.loadUserPort      = loadUserPort;
        this.loyaltyCalculator = loyaltyCalculator;
    }

    public List<EarningEntry> execute(Long userId) {
        LoyaltyProfile profile = loadUserPort.loadById(userId)
                .map(u -> u.loyalty())
                .orElse(LoyaltyProfile.empty());

        return loadOrderPort.loadRecentPaidByUserId(userId, RECENT_ORDERS_LIMIT)
                .stream()
                .map(order -> toEntry(order, profile))
                .toList();
    }

    private EarningEntry toEntry(Order order, LoyaltyProfile profile) {
        // Compute cashback using the user's current tier as an approximation
        int pointsEarned = 0;
        if (profile.tier() != null && profile.tier().cashbackPercentage() != null) {
            pointsEarned = order.totalAmount().amount()
                    .multiply(profile.tier().cashbackPercentage())
                    .divide(java.math.BigDecimal.valueOf(100), 0, java.math.RoundingMode.FLOOR)
                    .intValue();
        }
        return new EarningEntry(order.id(), pointsEarned, order.totalAmount().amount(), order.createdAt());
    }

    public record EarningEntry(Long orderId, int pointsEarned, java.math.BigDecimal orderAmount, Instant orderedAt) {}
}
