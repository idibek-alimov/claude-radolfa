package tj.radolfa.infrastructure.web.dto;

import java.util.List;

public record ApplyCouponResponseDto(
        boolean valid,
        Long discountId,
        List<String> affectedSkus,
        String invalidReason,
        CartDto cart
) {}
