package tj.radolfa.application.ports.in.discount;

public interface CheckCouponAvailabilityUseCase {

    /**
     * Returns {@code true} if the coupon code is not in use by any discount other than the
     * discount identified by {@code excludeDiscountId} (null-safe — use for create vs edit flows).
     */
    boolean isAvailable(String couponCode, Long excludeDiscountId);
}
