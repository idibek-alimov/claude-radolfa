package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.Cart;

public interface GetCartUseCase {
    Cart execute(Long userId);
}
