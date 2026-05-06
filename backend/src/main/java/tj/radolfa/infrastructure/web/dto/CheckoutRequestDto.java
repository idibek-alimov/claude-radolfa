package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.DeliveryType;

public record CheckoutRequestDto(
        int          loyaltyPointsToRedeem,
        String       notes,
        DeliveryType deliveryType,
        String       address,
        String       preferredTimeWindow,
        Long         pickpointId
) {
    public CheckoutRequestDto {
        if (loyaltyPointsToRedeem < 0) {
            throw new IllegalArgumentException("loyaltyPointsToRedeem cannot be negative");
        }
        if (deliveryType == null) {
            throw new IllegalArgumentException("deliveryType is required");
        }
    }
}
