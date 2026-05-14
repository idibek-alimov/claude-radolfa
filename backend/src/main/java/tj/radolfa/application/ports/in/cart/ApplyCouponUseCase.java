package tj.radolfa.application.ports.in.cart;

import tj.radolfa.application.readmodel.CartView;

import java.util.List;

public interface ApplyCouponUseCase {

    Result execute(Long userId, String couponCode);

    record Result(
            boolean valid,
            Long discountId,
            List<String> affectedSkus,
            String invalidReason,
            CartView cart
    ) {}
}
