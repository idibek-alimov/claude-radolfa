package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.cart.AddToCartUseCase;
import tj.radolfa.application.ports.out.LoadCartPort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.SaveCartPort;
import tj.radolfa.domain.model.Cart;
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
        int available = sku.getStockQuantity() != null ? sku.getStockQuantity() : 0;
        if (available < quantity) {
            throw new IllegalStateException(
                    "Insufficient stock for SKU " + skuId + ": requested=" + quantity + ", available=" + available);
        }

        Cart cart = loadCartPort.findActiveByUserId(userId)
                .orElseGet(() -> Cart.forUser(userId));

        cart.addItem(skuId, quantity, sku.getPrice());
        return saveCartPort.save(cart);
    }
}
