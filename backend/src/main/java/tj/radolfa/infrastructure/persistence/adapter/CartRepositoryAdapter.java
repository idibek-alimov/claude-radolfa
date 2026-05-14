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

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
            entity.setCouponCode(cart.getCouponCode());
        } else {
            entity = new CartEntity();
            entity.setUserId(cart.getUserId());
            entity.setStatus(cart.getStatus());
            entity.setCouponCode(cart.getCouponCode());
        }

        // Merge items: update existing rows in-place, remove stale, insert new.
        // Never clear+rebuild — Hibernate flushes inserts before deletes, which would
        // violate the UNIQUE(cart_id, sku_id) constraint when a SKU is already present.
        Set<Long> domainSkuIds = cart.getItems().stream()
                .map(CartItem::getSkuId)
                .collect(Collectors.toSet());

        entity.getItems().removeIf(e -> !domainSkuIds.contains(e.getSkuId()));

        Map<Long, CartItemEntity> existingBySkuId = entity.getItems().stream()
                .collect(Collectors.toMap(CartItemEntity::getSkuId, Function.identity()));

        for (CartItem item : cart.getItems()) {
            CartItemEntity itemEntity = existingBySkuId.get(item.getSkuId());
            if (itemEntity != null) {
                itemEntity.setQuantity(item.getQuantity());
                itemEntity.setUnitPriceSnapshot(item.getUnitPriceSnapshot().amount());
            } else {
                CartItemEntity newItem = new CartItemEntity();
                newItem.setCart(entity);
                newItem.setSkuId(item.getSkuId());
                newItem.setQuantity(item.getQuantity());
                newItem.setUnitPriceSnapshot(item.getUnitPriceSnapshot().amount());
                entity.getItems().add(newItem);
            }
        }

        return mapper.toCart(cartRepo.save(entity));
    }
}
