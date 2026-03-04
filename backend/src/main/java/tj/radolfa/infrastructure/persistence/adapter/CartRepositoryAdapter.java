package tj.radolfa.infrastructure.persistence.adapter;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadCartPort;
import tj.radolfa.application.ports.out.LoadSkuSnapshotPort;
import tj.radolfa.application.ports.out.SaveCartPort;
import tj.radolfa.domain.model.Cart;
import tj.radolfa.domain.model.CartItem;
import tj.radolfa.infrastructure.persistence.entity.CartEntity;
import tj.radolfa.infrastructure.persistence.entity.CartItemEntity;
import tj.radolfa.infrastructure.persistence.mappers.CartMapper;
import tj.radolfa.infrastructure.persistence.repository.CartRepository;
import tj.radolfa.infrastructure.persistence.repository.SkuRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class CartRepositoryAdapter implements LoadCartPort, SaveCartPort, LoadSkuSnapshotPort {

    private final CartRepository cartRepository;
    private final SkuRepository skuRepository;
    private final CartMapper cartMapper;
    private final EntityManager em;

    public CartRepositoryAdapter(CartRepository cartRepository,
                                 SkuRepository skuRepository,
                                 CartMapper cartMapper,
                                 EntityManager em) {
        this.cartRepository = cartRepository;
        this.skuRepository = skuRepository;
        this.cartMapper = cartMapper;
        this.em = em;
    }

    // ---- LoadCartPort ----

    @Override
    public Optional<Cart> loadCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .map(cartMapper::toCart);
    }

    // ---- SaveCartPort ----

    @Override
    public Cart saveCart(Cart cart) {
        CartEntity entity = cartRepository.findByUserId(cart.getUserId())
                .orElseGet(() -> {
                    CartEntity fresh = new CartEntity();
                    fresh.setUserId(cart.getUserId());
                    fresh.setItems(new ArrayList<>());
                    return fresh;
                });

        syncItems(entity, cart.getItems());

        CartEntity saved = cartRepository.save(entity);
        return cartMapper.toCart(saved);
    }

    /**
     * Synchronises the JPA item collection with the domain item list.
     *
     * <p>Uses orphanRemoval on the CartEntity side to automatically delete
     * items that are no longer present in the domain list.
     */
    private void syncItems(CartEntity entity, List<CartItem> domainItems) {
        // Remove items not present in the domain list
        entity.getItems().removeIf(existingItem ->
                domainItems.stream().noneMatch(di -> di.getSkuId().equals(existingItem.getSkuId())));

        for (CartItem domainItem : domainItems) {
            CartItemEntity existing = entity.getItems().stream()
                    .filter(e -> e.getSkuId().equals(domainItem.getSkuId()))
                    .findFirst()
                    .orElse(null);

            if (existing != null) {
                // Update quantity on existing JPA entity
                existing.setQuantity(domainItem.getQuantity());
                existing.setPriceSnapshot(domainItem.getPriceSnapshot());
            } else {
                // Add new item
                CartItemEntity newItem = cartMapper.toItemEntity(domainItem);
                newItem.setCart(entity);
                entity.getItems().add(newItem);
            }
        }
    }

    // ---- LoadSkuSnapshotPort ----

    /**
     * Loads the SKU snapshot by joining:
     * skus -> listing_variants -> product_bases (for productName)
     * skus -> listing_variants -> listing_variant_images (first image for imageUrl)
     *
     * <p>Price is sourced from ERPNext via the {@code price} column on the sku row.
     * This is the canonical source of truth — never override with Radolfa data.
     */
    @Override
    public SkuSnapshot load(Long skuId) {
        return skuRepository.findById(skuId).map(sku -> {
            var variant = sku.getListingVariant();
            String productName = variant.getProductBase() != null
                    ? variant.getProductBase().getName()
                    : null;
            String listingSlug = variant.getSlug();
            String sizeLabel = sku.getSizeLabel();

            // Take the first image ordered by sortOrder (matches @OrderBy on the collection)
            String imageUrl = variant.getImages() != null && !variant.getImages().isEmpty()
                    ? variant.getImages().get(0).getImageUrl()
                    : null;

            BigDecimal price = sku.getPrice() != null ? sku.getPrice() : BigDecimal.ZERO;

            return new SkuSnapshot(skuId, listingSlug, productName, sizeLabel, imageUrl, price);
        }).orElseThrow(() -> new IllegalArgumentException("SKU not found: " + skuId));
    }
}
