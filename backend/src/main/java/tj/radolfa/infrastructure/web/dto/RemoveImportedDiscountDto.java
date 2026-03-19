package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RemoveImportedDiscountDto(
        @NotBlank String externalRuleId
) {}
