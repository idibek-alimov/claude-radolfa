package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Cart;

import java.util.Optional;

public interface LoadCartPort {
    Optional<Cart> loadCart(Long userId);
}
