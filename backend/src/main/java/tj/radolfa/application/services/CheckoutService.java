package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.loyalty.RedeemLoyaltyPointsUseCase;
import tj.radolfa.application.ports.in.order.CheckoutUseCase;
import tj.radolfa.application.ports.out.LoadCartPort;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveCartPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.application.ports.out.StockAdjustmentPort;
import tj.radolfa.domain.model.Cart;
import tj.radolfa.domain.model.CartItem;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.Sku;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.service.LoyaltyCalculator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class CheckoutService implements CheckoutUseCase {

    private final LoadCartPort               loadCartPort;
    private final SaveCartPort               saveCartPort;
    private final LoadSkuPort                loadSkuPort;
    private final LoadListingVariantPort     loadListingVariantPort;
    private final LoadProductBasePort        loadProductBasePort;
    private final LoadUserPort               loadUserPort;
    private final SaveOrderPort              saveOrderPort;
    private final StockAdjustmentPort        stockAdjustmentPort;
    private final LoyaltyCalculator          loyaltyCalculator;
    private final RedeemLoyaltyPointsUseCase redeemLoyaltyPointsUseCase;

    public CheckoutService(LoadCartPort loadCartPort,
                           SaveCartPort saveCartPort,
                           LoadSkuPort loadSkuPort,
                           LoadListingVariantPort loadListingVariantPort,
                           LoadProductBasePort loadProductBasePort,
                           LoadUserPort loadUserPort,
                           SaveOrderPort saveOrderPort,
                           StockAdjustmentPort stockAdjustmentPort,
                           LoyaltyCalculator loyaltyCalculator,
                           RedeemLoyaltyPointsUseCase redeemLoyaltyPointsUseCase) {
        this.loadCartPort               = loadCartPort;
        this.saveCartPort               = saveCartPort;
        this.loadSkuPort                = loadSkuPort;
        this.loadListingVariantPort     = loadListingVariantPort;
        this.loadProductBasePort        = loadProductBasePort;
        this.loadUserPort               = loadUserPort;
        this.saveOrderPort              = saveOrderPort;
        this.stockAdjustmentPort        = stockAdjustmentPort;
        this.loyaltyCalculator          = loyaltyCalculator;
        this.redeemLoyaltyPointsUseCase = redeemLoyaltyPointsUseCase;
    }

    @Override
    @Transactional
    public Result execute(Command command) {
        // 1. Load user with loyalty profile
        User user = loadUserPort.loadById(command.userId())
                .orElseThrow(() -> new IllegalStateException("User not found: " + command.userId()));

        // 2. Load active cart, validate non-empty
        Cart cart = loadCartPort.findActiveByUserId(command.userId())
                .orElseThrow(() -> new IllegalStateException(
                        "No active cart for user: " + command.userId()));
        if (cart.isEmpty()) {
            throw new IllegalStateException("Cannot checkout with an empty cart");
        }

        // 3. Re-validate all items are still in stock
        for (CartItem item : cart.getItems()) {
            Sku sku = loadSkuPort.findSkuById(item.getSkuId())
                    .orElseThrow(() -> new IllegalStateException("SKU not found: " + item.getSkuId()));
            int available = sku.getStockQuantity() != null ? sku.getStockQuantity() : 0;
            if (available < item.getQuantity()) {
                throw new IllegalStateException(
                        "Insufficient stock for SKU " + item.getSkuId() +
                        " (available=" + available + ", requested=" + item.getQuantity() + ")");
            }
        }

        // 4. Compute subtotal from price snapshots
        Money subtotal = cart.total();

        // 5. Apply tier discount
        LoyaltyProfile profile = user.loyalty();
        Money tierDiscount = loyaltyCalculator.resolveDiscount(profile, subtotal);
        BigDecimal afterTierRaw = subtotal.amount()
                .subtract(tierDiscount.amount())
                .max(BigDecimal.ZERO);
        Money afterTierDiscount = new Money(afterTierRaw);

        // 6. Apply points redemption (pessimistic deduct before order commit)
        Money pointsDiscount = Money.ZERO;
        int pointsToRedeem = command.loyaltyPointsToRedeem();
        if (pointsToRedeem > 0) {
            int maxRedeemable = loyaltyCalculator.maxRedeemablePoints(profile, afterTierDiscount);
            if (pointsToRedeem > maxRedeemable) {
                throw new IllegalArgumentException(
                        "Cannot redeem " + pointsToRedeem + " points (max=" + maxRedeemable + ")");
            }
            pointsDiscount = redeemLoyaltyPointsUseCase.execute(command.userId(), pointsToRedeem);
        }

        // 7. Final total (floor at 0 — cannot go negative from discounts)
        BigDecimal totalRaw = afterTierRaw
                .subtract(pointsDiscount.amount())
                .max(BigDecimal.ZERO);
        Money total = new Money(totalRaw);

        // 8. Build order items enriched with skuCode and productName
        List<OrderItem> orderItems = cart.getItems().stream()
                .map(this::enrichToOrderItem)
                .toList();

        // 9. Persist order with PENDING status
        Order newOrder = new Order(null, command.userId(), null,
                OrderStatus.PENDING, total, orderItems, Instant.now());
        Order saved = saveOrderPort.save(newOrder);

        // 10. Decrement stock for each item
        for (CartItem item : cart.getItems()) {
            stockAdjustmentPort.decrement(item.getSkuId(), item.getQuantity());
        }

        // 11. Transition cart to CHECKED_OUT
        cart.checkout();
        saveCartPort.save(cart);

        return new Result(saved.id(), subtotal, tierDiscount, pointsDiscount, total);
    }

    private OrderItem enrichToOrderItem(CartItem cartItem) {
        Sku sku = loadSkuPort.findSkuById(cartItem.getSkuId())
                .orElseThrow(() -> new IllegalStateException("SKU not found: " + cartItem.getSkuId()));

        ListingVariant variant = loadListingVariantPort.findVariantById(sku.getListingVariantId())
                .orElseThrow(() -> new IllegalStateException(
                        "Variant not found: " + sku.getListingVariantId()));

        ProductBase product = loadProductBasePort.findById(variant.getProductBaseId())
                .orElseThrow(() -> new IllegalStateException(
                        "Product not found: " + variant.getProductBaseId()));

        return new OrderItem(null, cartItem.getSkuId(), sku.getSkuCode(),
                product.getName(), cartItem.getQuantity(), cartItem.getUnitPriceSnapshot());
    }
}
