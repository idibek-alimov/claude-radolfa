package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import tj.radolfa.domain.model.UserRole;

import java.math.BigDecimal;

public record ImportUserPayload(
        @NotBlank String phone,
        String name,
        String email,
        UserRole role,
        Boolean enabled,
        Integer loyaltyPoints,
        String tierName,
        BigDecimal spendToNextTier,
        BigDecimal spendToMaintainTier,
        BigDecimal currentMonthSpending) {
}
