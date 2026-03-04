package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.Cart;

public interface AddToCartUseCase {
    Cart execute(Long userId, Long skuId, int quantity);
}
