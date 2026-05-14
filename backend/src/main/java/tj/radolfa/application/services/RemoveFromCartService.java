package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.cart.RemoveFromCartUseCase;
import tj.radolfa.application.ports.out.LoadCartPort;
import tj.radolfa.application.ports.out.SaveCartPort;
import tj.radolfa.domain.model.Cart;

@Service
public class RemoveFromCartService implements RemoveFromCartUseCase {

    private final LoadCartPort loadCartPort;
    private final SaveCartPort saveCartPort;

    public RemoveFromCartService(LoadCartPort loadCartPort, SaveCartPort saveCartPort) {
        this.loadCartPort = loadCartPort;
        this.saveCartPort = saveCartPort;
    }

    @Override
    @Transactional
    public Cart execute(Long userId, Long skuId) {
        Cart cart = loadCartPort.findActiveByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No active cart for user: " + userId));
        cart.removeItem(skuId);
        return saveCartPort.save(cart);
    }
}
