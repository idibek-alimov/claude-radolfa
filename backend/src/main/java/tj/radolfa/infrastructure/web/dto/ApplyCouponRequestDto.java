package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplyCouponRequestDto(
        @NotBlank @Size(max = 64) String couponCode
) {}
