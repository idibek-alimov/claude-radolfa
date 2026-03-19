package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.cart.UpdateCartItemQuantityUseCase;
import tj.radolfa.application.ports.out.LoadCartPort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.SaveCartPort;
import tj.radolfa.domain.model.Cart;

@Service
public class UpdateCartItemQuantityService implements UpdateCartItemQuantityUseCase {

    private final LoadCartPort loadCartPort;
    private final SaveCartPort saveCartPort;
    private final LoadSkuPort  loadSkuPort;

    public UpdateCartItemQuantityService(LoadCartPort loadCartPort,
                                         SaveCartPort saveCartPort,
                                         LoadSkuPort loadSkuPort) {
        this.loadCartPort = loadCartPort;
        this.saveCartPort = saveCartPort;
        this.loadSkuPort  = loadSkuPort;
    }

    @Override
    @Transactional
    public Cart execute(Long userId, Long skuId, int newQuantity) {
        Cart cart = loadCartPort.findActiveByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No active cart for user: " + userId));

        if (newQuantity > 0) {
            int available = loadSkuPort.findSkuById(skuId)
                    .map(s -> s.getStockQuantity() != null ? s.getStockQuantity() : 0)
                    .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + skuId));

            if (available < newQuantity) {
                throw new IllegalStateException(
                        "Insufficient stock for SKU " + skuId + ": requested=" + newQuantity + ", available=" + available);
            }
        }

        cart.updateQuantity(skuId, newQuantity);
        return saveCartPort.save(cart);
    }
}
