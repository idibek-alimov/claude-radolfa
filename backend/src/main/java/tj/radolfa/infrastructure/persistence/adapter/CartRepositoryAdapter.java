package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.out.LoadCartPort;
import tj.radolfa.application.ports.out.SaveCartPort;
import tj.radolfa.domain.model.Cart;
import tj.radolfa.domain.model.CartItem;
import tj.radolfa.domain.model.CartStatus;
import tj.radolfa.infrastructure.persistence.entity.CartEntity;
import tj.radolfa.infrastructure.persistence.entity.CartItemEntity;
import tj.radolfa.infrastructure.persistence.mappers.CartMapper;
import tj.radolfa.infrastructure.persistence.repository.CartRepository;

import java.util.Optional;

@Component
public class CartRepositoryAdapter implements LoadCartPort, SaveCartPort {

    private final CartRepository cartRepo;
    private final CartMapper mapper;

    public CartRepositoryAdapter(CartRepository cartRepo, CartMapper mapper) {
        this.cartRepo = cartRepo;
        this.mapper = mapper;
    }

    // ---- LoadCartPort ----

    @Override
    public Optional<Cart> findActiveByUserId(Long userId) {
        return cartRepo.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .map(mapper::toCart);
    }

    @Override
    public Optional<Cart> findById(Long cartId) {
        return cartRepo.findById(cartId).map(mapper::toCart);
    }

    // ---- SaveCartPort ----

    @Override
    @Transactional
    public Cart save(Cart cart) {
        CartEntity entity;

        if (cart.getId() != null) {
            entity = cartRepo.findById(cart.getId())
                    .orElseThrow(() -> new IllegalStateException("Cart not found: " + cart.getId()));
            entity.setStatus(cart.getStatus());
        } else {
            entity = new CartEntity();
            entity.setUserId(cart.getUserId());
            entity.setStatus(cart.getStatus());
        }

        // Sync items: clear + rebuild from domain state
        entity.getItems().clear();
        for (CartItem item : cart.getItems()) {
            CartItemEntity itemEntity = new CartItemEntity();
            itemEntity.setCart(entity);
            itemEntity.setSkuId(item.getSkuId());
            itemEntity.setQuantity(item.getQuantity());
            itemEntity.setUnitPriceSnapshot(item.getUnitPriceSnapshot().amount());
            entity.getItems().add(itemEntity);
        }

        return mapper.toCart(cartRepo.save(entity));
    }
}
