package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RemoveDiscountPayload(
        @NotBlank String erpPricingRuleId
) {}
