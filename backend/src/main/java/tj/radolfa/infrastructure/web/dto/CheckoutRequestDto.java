package tj.radolfa.infrastructure.web.dto;

public record CheckoutRequestDto(
        int    loyaltyPointsToRedeem,  // 0 = no redemption
        String notes                   // optional customer note
) {
    public CheckoutRequestDto {
        if (loyaltyPointsToRedeem < 0) {
            throw new IllegalArgumentException("loyaltyPointsToRedeem cannot be negative");
        }
    }
}
