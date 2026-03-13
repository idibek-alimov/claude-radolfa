package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.LoyaltyTier;

import java.math.BigDecimal;

public record LoyaltyTierDto(
        String name,
        BigDecimal discountPercentage,
        BigDecimal cashbackPercentage,
        BigDecimal minSpendRequirement,
        int rank
) {
    public static LoyaltyTierDto fromDomain(LoyaltyTier tier) {
        if (tier == null) return null;
        return new LoyaltyTierDto(
                tier.name(),
                tier.discountPercentage(),
                tier.cashbackPercentage(),
                tier.minSpendRequirement(),
                tier.rank());
    }
}
