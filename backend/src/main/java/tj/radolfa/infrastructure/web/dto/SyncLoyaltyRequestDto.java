package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * DTO for ERPNext loyalty points sync.
 * Phone is the unique lookup key.
 */
public record SyncLoyaltyRequestDto(
        @NotBlank String phone,
        @PositiveOrZero int points,
        String tierName,
        BigDecimal spendToNextTier,
        BigDecimal spendToMaintainTier,
        BigDecimal currentMonthSpending
) {}
