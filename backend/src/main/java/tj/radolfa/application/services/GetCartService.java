package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.cart.GetCartUseCase;
import tj.radolfa.application.ports.out.LoadCartPort;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.readmodel.CartView;
import tj.radolfa.domain.model.Cart;
import tj.radolfa.domain.model.CartItem;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.Sku;

import java.util.List;

@Service
public class GetCartService implements GetCartUseCase {

    private final LoadCartPort            loadCartPort;
    private final LoadSkuPort             loadSkuPort;
    private final LoadListingVariantPort  loadListingVariantPort;
    private final LoadProductBasePort     loadProductBasePort;

    public GetCartService(LoadCartPort loadCartPort,
                          LoadSkuPort loadSkuPort,
                          LoadListingVariantPort loadListingVariantPort,
                          LoadProductBasePort loadProductBasePort) {
        this.loadCartPort           = loadCartPort;
        this.loadSkuPort            = loadSkuPort;
        this.loadListingVariantPort = loadListingVariantPort;
        this.loadProductBasePort    = loadProductBasePort;
    }

    @Override
    @Transactional(readOnly = true)
    public CartView execute(Long userId) {
        return loadCartPort.findActiveByUserId(userId)
                .map(this::toView)
                .orElse(CartView.empty());
    }

    private CartView toView(Cart cart) {
        List<CartView.ItemView> itemViews = cart.getItems().stream()
                .map(this::enrichItem)
                .toList();
        return new CartView(cart.getId(), itemViews, cart.total(), cart.itemCount(), cart.getCouponCode());
    }

    private CartView.ItemView enrichItem(CartItem item) {
        Sku sku = loadSkuPort.findSkuById(item.getSkuId())
                .orElseThrow(() -> new IllegalStateException("SKU not found: " + item.getSkuId()));

        ListingVariant variant = loadListingVariantPort.findVariantById(sku.getListingVariantId())
                .orElseThrow(() -> new IllegalStateException("Variant not found: " + sku.getListingVariantId()));

        ProductBase product = loadProductBasePort.findById(variant.getProductBaseId())
                .orElseThrow(() -> new IllegalStateException("Product not found: " + variant.getProductBaseId()));

        String imageUrl = variant.getImages().isEmpty() ? null : variant.getImages().get(0);
        int stock = sku.getStockQuantity() != null ? sku.getStockQuantity() : 0;

        return new CartView.ItemView(
                item.getSkuId(),
                product.getName(),
                variant.getColorKey(),
                sku.getSizeLabel(),
                imageUrl,
                item.getQuantity(),
                item.getUnitPriceSnapshot(),
                item.lineTotal(),
                stock,
                stock > 0
        );
    }
}
