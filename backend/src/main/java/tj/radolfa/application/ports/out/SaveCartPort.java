package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Cart;

public interface SaveCartPort {
    Cart saveCart(Cart cart);
}
