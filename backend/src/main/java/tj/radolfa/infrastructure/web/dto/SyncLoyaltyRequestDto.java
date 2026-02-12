package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * DTO for ERPNext loyalty points sync.
 * Phone is the unique lookup key.
 */
public record SyncLoyaltyRequestDto(
        @NotBlank String phone,
        @PositiveOrZero int points
) {}
