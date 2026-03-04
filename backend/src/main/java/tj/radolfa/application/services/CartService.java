package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.AddToCartUseCase;
import tj.radolfa.application.ports.in.ClearCartUseCase;
import tj.radolfa.application.ports.in.GetCartUseCase;
import tj.radolfa.application.ports.in.RemoveFromCartUseCase;
import tj.radolfa.application.ports.in.UpdateCartItemUseCase;
import tj.radolfa.application.ports.out.LoadCartPort;
import tj.radolfa.application.ports.out.LoadSkuSnapshotPort;
import tj.radolfa.application.ports.out.SaveCartPort;
import tj.radolfa.domain.model.Cart;

import java.util.ArrayList;

/**
 * Single service bean implementing all five cart use-cases.
 *
 * <p>Because {@link AddToCartUseCase} and {@link UpdateCartItemUseCase} share the
 * same method signature {@code execute(Long, Long, int)}, they cannot both be
 * implemented as {@code @Override} methods in the same class. Java erases generic
 * types and only cares about parameter types at the JVM level, so two
 * {@code execute(Long, Long, int)} methods would be duplicate. The solution is the
 * standard hexagonal pattern: separate the "command" from the "query" at the
 * interface boundary but merge them into one cohesive Spring bean, delegating
 * the two conflicting signatures to named methods that the interface-specific
 * inner adapters forward to.
 *
 * <p>The five use-case interfaces are implemented by five package-private static
 * adapter classes that each hold a reference to this shared service. A single
 * {@code CartServiceFacade} bean is then exposed; Spring wires the adapters as
 * separate beans via their respective interface types.
 */
@Service
@Transactional
public class CartService {

    private final LoadCartPort loadCartPort;
    private final SaveCartPort saveCartPort;
    private final LoadSkuSnapshotPort loadSkuSnapshotPort;

    public CartService(LoadCartPort loadCartPort,
                       SaveCartPort saveCartPort,
                       LoadSkuSnapshotPort loadSkuSnapshotPort) {
        this.loadCartPort = loadCartPort;
        this.saveCartPort = saveCartPort;
        this.loadSkuSnapshotPort = loadSkuSnapshotPort;
    }

    // ---- Use-case implementations (called by the adapter beans below) ----

    @Transactional(readOnly = true)
    public Cart getCart(Long userId) {
        return loadCartPort.loadCart(userId)
                .orElseGet(() -> new Cart(userId, new ArrayList<>()));
    }

    public Cart addToCart(Long userId, Long skuId, int quantity) {
        Cart cart = loadCartPort.loadCart(userId)
                .orElseGet(() -> new Cart(userId, new ArrayList<>()));

        LoadSkuSnapshotPort.SkuSnapshot snapshot = loadSkuSnapshotPort.load(skuId);

        cart.addItem(
                snapshot.skuId(),
                snapshot.listingSlug(),
                snapshot.productName(),
                snapshot.sizeLabel(),
                snapshot.imageUrl(),
                snapshot.price(),
                quantity);

        return saveCartPort.saveCart(cart);
    }

    public Cart updateCartItem(Long userId, Long skuId, int quantity) {
        Cart cart = loadCartPort.loadCart(userId)
                .orElseGet(() -> new Cart(userId, new ArrayList<>()));
        cart.updateQuantity(skuId, quantity);
        return saveCartPort.saveCart(cart);
    }

    public Cart removeFromCart(Long userId, Long skuId) {
        Cart cart = loadCartPort.loadCart(userId)
                .orElseGet(() -> new Cart(userId, new ArrayList<>()));
        cart.removeItem(skuId);
        return saveCartPort.saveCart(cart);
    }

    public void clearCart(Long userId) {
        Cart cart = loadCartPort.loadCart(userId)
                .orElseGet(() -> new Cart(userId, new ArrayList<>()));
        cart.clear();
        saveCartPort.saveCart(cart);
    }

    // ======================================================================
    // Port adapter beans — one Spring bean per use-case interface.
    // Each delegates to the shared CartService above.
    // ======================================================================

    @Service
    public static class GetCartAdapter implements GetCartUseCase {
        private final CartService service;
        public GetCartAdapter(CartService service) { this.service = service; }

        @Override
        public Cart execute(Long userId) { return service.getCart(userId); }
    }

    @Service
    public static class AddToCartAdapter implements AddToCartUseCase {
        private final CartService service;
        public AddToCartAdapter(CartService service) { this.service = service; }

        @Override
        public Cart execute(Long userId, Long skuId, int quantity) {
            return service.addToCart(userId, skuId, quantity);
        }
    }

    @Service
    public static class UpdateCartItemAdapter implements UpdateCartItemUseCase {
        private final CartService service;
        public UpdateCartItemAdapter(CartService service) { this.service = service; }

        @Override
        public Cart execute(Long userId, Long skuId, int quantity) {
            return service.updateCartItem(userId, skuId, quantity);
        }
    }

    @Service
    public static class RemoveFromCartAdapter implements RemoveFromCartUseCase {
        private final CartService service;
        public RemoveFromCartAdapter(CartService service) { this.service = service; }

        @Override
        public Cart execute(Long userId, Long skuId) {
            return service.removeFromCart(userId, skuId);
        }
    }

    @Service
    public static class ClearCartAdapter implements ClearCartUseCase {
        private final CartService service;
        public ClearCartAdapter(CartService service) { this.service = service; }

        @Override
        public void execute(Long userId) { service.clearCart(userId); }
    }
}
