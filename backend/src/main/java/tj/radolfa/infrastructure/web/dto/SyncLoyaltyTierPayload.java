package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record SyncLoyaltyTierPayload(
        @NotBlank String name,
        @NotNull @PositiveOrZero BigDecimal discountPercentage,
        @NotNull @PositiveOrZero BigDecimal cashbackPercentage,
        @NotNull @PositiveOrZero BigDecimal minSpendRequirement,
        int rank) {
}
