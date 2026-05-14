package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.cart.GetCartUseCase;
import tj.radolfa.application.ports.in.cart.RemoveCouponUseCase;
import tj.radolfa.application.ports.out.LoadCartPort;
import tj.radolfa.application.ports.out.SaveCartPort;
import tj.radolfa.application.readmodel.CartView;
import tj.radolfa.domain.model.Cart;

@Service
public class RemoveCouponService implements RemoveCouponUseCase {

    private final LoadCartPort loadCartPort;
    private final SaveCartPort saveCartPort;
    private final GetCartUseCase getCartUseCase;

    public RemoveCouponService(LoadCartPort loadCartPort,
                               SaveCartPort saveCartPort,
                               GetCartUseCase getCartUseCase) {
        this.loadCartPort  = loadCartPort;
        this.saveCartPort  = saveCartPort;
        this.getCartUseCase = getCartUseCase;
    }

    @Override
    @Transactional
    public CartView execute(Long userId) {
        Cart cart = loadCartPort.findActiveByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No active cart for user: " + userId));
        cart.removeCoupon();
        saveCartPort.save(cart);
        return getCartUseCase.execute(userId);
    }
}
