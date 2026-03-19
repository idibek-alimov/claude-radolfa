package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.application.services.GetRecentEarningsService.EarningEntry;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * DTO representing user information in API responses.
 */
public record UserDto(
        Long id,
        String phone,
        String role,
        String name,
        String email,
        LoyaltyDto loyalty,
        boolean enabled
) {
    public record RecentEarningDto(Long orderId, int pointsEarned, BigDecimal orderAmount, Instant orderedAt) {
        public static RecentEarningDto from(EarningEntry e) {
            return new RecentEarningDto(e.orderId(), e.pointsEarned(), e.orderAmount(), e.orderedAt());
        }
    }

    public record LoyaltyDto(
            int points,
            LoyaltyTierDto tier,
            BigDecimal spendToNextTier,
            BigDecimal spendToMaintainTier,
            BigDecimal currentMonthSpending,
            List<RecentEarningDto> recentEarnings
    ) {}

    public static UserDto fromDomain(User user) {
        return fromDomain(user, List.of());
    }

    public static UserDto fromDomain(User user, List<EarningEntry> recentEarnings) {
        LoyaltyProfile lp = user.loyalty();
        LoyaltyDto loyalty = lp != null
                ? new LoyaltyDto(
                        lp.points(),
                        LoyaltyTierDto.fromDomain(lp.tier()),
                        lp.spendToNextTier(),
                        lp.spendToMaintainTier(),
                        lp.currentMonthSpending(),
                        recentEarnings.stream().map(RecentEarningDto::from).toList())
                : new LoyaltyDto(0, null, null, null, null, List.of());

        return new UserDto(
                user.id(),
                user.phone().value(),
                user.role().name(),
                user.name(),
                user.email(),
                loyalty,
                user.enabled()
        );
    }
}
