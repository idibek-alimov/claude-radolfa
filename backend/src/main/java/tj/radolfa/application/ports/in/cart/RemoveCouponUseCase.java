package tj.radolfa.application.ports.in.cart;

import tj.radolfa.application.readmodel.CartView;

public interface RemoveCouponUseCase {

    CartView execute(Long userId);
}
