package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.cart.GetCartUseCase;
import tj.radolfa.application.ports.in.discount.ResolveDiscountsUseCase;
import tj.radolfa.application.ports.out.LoadCartPort;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.readmodel.CartView;
import tj.radolfa.domain.model.Cart;
import tj.radolfa.domain.model.CartItem;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.Sku;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.WinningMechanism;
import tj.radolfa.domain.service.CartLinePricer;
import tj.radolfa.domain.service.LoyaltyCalculator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GetCartService implements GetCartUseCase {

    private final LoadCartPort            loadCartPort;
    private final LoadSkuPort             loadSkuPort;
    private final LoadListingVariantPort  loadListingVariantPort;
    private final LoadProductBasePort     loadProductBasePort;
    private final LoadUserPort            loadUserPort;
    private final LoyaltyCalculator       loyaltyCalculator;
    private final ResolveDiscountsUseCase resolveDiscountsUseCase;
    private final CartLinePricer          cartLinePricer;

    public GetCartService(LoadCartPort loadCartPort,
                          LoadSkuPort loadSkuPort,
                          LoadListingVariantPort loadListingVariantPort,
                          LoadProductBasePort loadProductBasePort,
                          LoadUserPort loadUserPort,
                          LoyaltyCalculator loyaltyCalculator,
                          ResolveDiscountsUseCase resolveDiscountsUseCase,
                          CartLinePricer cartLinePricer) {
        this.loadCartPort            = loadCartPort;
        this.loadSkuPort             = loadSkuPort;
        this.loadListingVariantPort  = loadListingVariantPort;
        this.loadProductBasePort     = loadProductBasePort;
        this.loadUserPort            = loadUserPort;
        this.loyaltyCalculator       = loyaltyCalculator;
        this.resolveDiscountsUseCase = resolveDiscountsUseCase;
        this.cartLinePricer          = cartLinePricer;
    }

    @Override
    @Transactional(readOnly = true)
    public CartView execute(Long userId) {
        return loadCartPort.findActiveByUserId(userId)
                .map(cart -> toView(cart, userId))
                .orElse(CartView.empty());
    }

    private CartView toView(Cart cart, Long userId) {
        if (cart.getItems().isEmpty()) {
            return new CartView(cart.getId(), List.of(), Money.ZERO, 0,
                    cart.getCouponCode(), cart.getPendingOrderId(), CartView.Summary.empty());
        }

        Set<Long> skuIds = cart.getItems().stream().map(CartItem::getSkuId).collect(Collectors.toSet());
        Map<Long, Sku> skuById = loadSkuPort.findAllByIdsAsMap(skuIds);

        Set<Long> variantIds = skuById.values().stream()
                .map(Sku::getListingVariantId)
                .collect(Collectors.toSet());
        Map<Long, ListingVariant> variantById = loadListingVariantPort.findVariantsByIds(variantIds);

        Set<Long> productIds = variantById.values().stream()
                .map(ListingVariant::getProductBaseId)
                .collect(Collectors.toSet());
        Map<Long, ProductBase> productById = loadProductBasePort.findProductsByIds(productIds);

        User user = loadUserPort.loadById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
        BigDecimal tierPct = loyaltyCalculator.resolveTierPercentage(user.loyalty());

        List<String> itemCodes = skuById.values().stream()
                .map(Sku::getSkuCode)
                .distinct()
                .toList();
        Map<String, BigDecimal> priceByCode = skuById.values().stream()
                .filter(s -> s.getPrice() != null && s.getPrice().amount() != null)
                .collect(Collectors.toMap(Sku::getSkuCode, s -> s.getPrice().amount(), (a, b) -> a));

        Map<String, List<Discount>> resolvedDiscounts = resolveDiscountsUseCase.resolve(
                new ResolveDiscountsUseCase.Query(
                        itemCodes, userId, cart.total().amount(), cart.getCouponCode(), priceByCode));

        List<CartView.ItemView> itemViews = cart.getItems().stream()
                .map(item -> enrichItem(item, skuById, variantById, productById, tierPct, resolvedDiscounts))
                .toList();

        CartView.Summary summary = buildSummary(itemViews);

        return new CartView(cart.getId(), itemViews, summary.total(), cart.itemCount(),
                cart.getCouponCode(), cart.getPendingOrderId(), summary);
    }

    private CartView.ItemView enrichItem(CartItem item,
                                          Map<Long, Sku> skuById,
                                          Map<Long, ListingVariant> variantById,
                                          Map<Long, ProductBase> productById,
                                          BigDecimal tierPct,
                                          Map<String, List<Discount>> resolvedDiscounts) {
        Sku sku = skuById.get(item.getSkuId());
        if (sku == null) throw new IllegalStateException("SKU not found: " + item.getSkuId());

        ListingVariant variant = variantById.get(sku.getListingVariantId());
        if (variant == null) throw new IllegalStateException("Variant not found: " + sku.getListingVariantId());

        ProductBase product = productById.get(variant.getProductBaseId());
        if (product == null) throw new IllegalStateException("Product not found: " + variant.getProductBaseId());

        String imageUrl = variant.getImages().isEmpty() ? null : variant.getImages().get(0);
        int stock = sku.getStockQuantity() != null ? sku.getStockQuantity() : 0;

        List<Discount> discounts = resolvedDiscounts.getOrDefault(sku.getSkuCode(), List.of());
        CartLinePricer.LinePrice linePrice =
                cartLinePricer.price(item.getUnitPriceSnapshot(), tierPct, discounts);

        Integer discountPercent = linePrice.mechanism() == WinningMechanism.NONE
                ? null
                : linePrice.effectivePercent().intValue();

        return new CartView.ItemView(
                item.getSkuId(),
                product.getName(),
                variant.getColorKey(),
                sku.getSizeLabel(),
                imageUrl,
                item.getQuantity(),
                linePrice.finalUnitPrice(),
                linePrice.finalUnitPrice().multiply(item.getQuantity()),
                stock,
                stock > 0,
                item.getUnitPriceSnapshot(),
                discountPercent,
                linePrice.mechanism(),
                product.getCategory()
        );
    }

    /**
     * Partitions each line's savings by which mechanism won it. Built so that
     * {@code subtotal - total == itemDiscounts + crownTier} holds exactly: every
     * line contributes {@code (original - final) × qty} to exactly one bucket
     * (or to neither, when {@code mechanism == NONE} and the amount is zero).
     */
    private CartView.Summary buildSummary(List<CartView.ItemView> itemViews) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal itemDiscounts = BigDecimal.ZERO;
        BigDecimal crownTier = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        for (CartView.ItemView item : itemViews) {
            BigDecimal qty = BigDecimal.valueOf(item.quantity());
            BigDecimal original = item.originalUnitPrice().amount();
            BigDecimal finalPrice = item.unitPrice().amount();

            subtotal = subtotal.add(original.multiply(qty));
            total = total.add(finalPrice.multiply(qty));

            BigDecimal lineSavings = original.subtract(finalPrice).multiply(qty);
            if (item.mechanism() == WinningMechanism.CAMPAIGN) {
                itemDiscounts = itemDiscounts.add(lineSavings);
            } else if (item.mechanism() == WinningMechanism.LOYALTY) {
                crownTier = crownTier.add(lineSavings);
            }
        }

        return new CartView.Summary(
                new Money(subtotal),
                new Money(itemDiscounts),
                new Money(crownTier),
                Money.ZERO,
                new Money(total),
                new Money(itemDiscounts.add(crownTier))
        );
    }
}
