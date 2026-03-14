package tj.radolfa.domain.model;

import java.math.BigDecimal;

public record LoyaltyTier(
        Long id,
        String name,
        BigDecimal discountPercentage,
        BigDecimal cashbackPercentage,
        BigDecimal minSpendRequirement,
        int displayOrder,
        String color,
        Long version
) {}
