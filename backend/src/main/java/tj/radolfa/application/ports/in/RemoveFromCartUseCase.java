package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.Cart;

public interface RemoveFromCartUseCase {
    Cart execute(Long userId, Long skuId);
}
