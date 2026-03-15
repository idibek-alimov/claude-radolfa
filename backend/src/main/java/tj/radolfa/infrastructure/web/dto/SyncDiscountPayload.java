package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record SyncDiscountPayload(
        @NotBlank String erpPricingRuleId,
        @NotBlank String itemCode,
        @NotNull BigDecimal discountValue,
        @NotNull Instant validFrom,
        @NotNull Instant validUpto,
        boolean disabled
) {}
