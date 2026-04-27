package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.cart.AddToCartUseCase;
import tj.radolfa.application.ports.out.LoadCartPort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.SaveCartPort;
import tj.radolfa.domain.model.Cart;
import tj.radolfa.domain.model.CartItem;
import tj.radolfa.domain.model.Sku;

@Service
public class AddToCartService implements AddToCartUseCase {

    private final LoadCartPort loadCartPort;
    private final SaveCartPort saveCartPort;
    private final LoadSkuPort loadSkuPort;

    public AddToCartService(LoadCartPort loadCartPort,
                            SaveCartPort saveCartPort,
                            LoadSkuPort loadSkuPort) {
        this.loadCartPort = loadCartPort;
        this.saveCartPort = saveCartPort;
        this.loadSkuPort  = loadSkuPort;
    }

    @Override
    @Transactional
    public Cart execute(Long userId, Long skuId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }

        Sku sku = loadSkuPort.findSkuById(skuId)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + skuId));

        if (sku.getPrice() == null) {
            throw new IllegalStateException("SKU has no price set: " + skuId);
        }

        Cart cart = loadCartPort.findActiveByUserId(userId)
                .orElseGet(() -> Cart.forUser(userId));

        int available = sku.getStockQuantity() != null ? sku.getStockQuantity() : 0;
        int alreadyInCart = cart.getItems().stream()
                .filter(i -> i.getSkuId().equals(skuId))
                .mapToInt(CartItem::getQuantity)
                .sum();
        if (alreadyInCart + quantity > available) {
            throw new IllegalStateException(
                    "Insufficient stock for SKU " + skuId
                    + ": requested=" + (alreadyInCart + quantity)
                    + ", available=" + available);
        }

        cart.addItem(skuId, quantity, sku.getPrice());
        return saveCartPort.save(cart);
    }
}
