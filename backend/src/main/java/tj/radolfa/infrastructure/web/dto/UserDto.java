package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.User;

import java.math.BigDecimal;

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
    public record LoyaltyDto(
            int points,
            LoyaltyTierDto tier,
            BigDecimal spendToNextTier,
            BigDecimal spendToMaintainTier,
            BigDecimal currentMonthSpending
    ) {}

    public static UserDto fromDomain(User user) {
        LoyaltyProfile lp = user.loyalty();
        LoyaltyDto loyalty = lp != null
                ? new LoyaltyDto(
                        lp.points(),
                        LoyaltyTierDto.fromDomain(lp.tier()),
                        lp.spendToNextTier(),
                        lp.spendToMaintainTier(),
                        lp.currentMonthSpending())
                : new LoyaltyDto(0, null, null, null, null);

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
