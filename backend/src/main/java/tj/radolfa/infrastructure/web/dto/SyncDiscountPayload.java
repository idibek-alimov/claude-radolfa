package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SyncDiscountPayload(
        @NotBlank String erpPricingRuleId,
        @NotEmpty List<@NotBlank String> itemCodes,
        @NotNull
        @DecimalMin(value = "0.00", message = "Discount value must be >= 0")
        @DecimalMax(value = "100.00", message = "Discount value must be <= 100")
        BigDecimal discountValue,
        @NotNull Instant validFrom,
        @NotNull Instant validUpto,
        boolean disabled,
        String title,
        String colorHex
) {}
