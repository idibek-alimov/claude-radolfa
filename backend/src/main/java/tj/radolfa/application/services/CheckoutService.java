package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.loyalty.RedeemLoyaltyPointsUseCase;
import tj.radolfa.application.ports.in.order.CheckoutUseCase;
import tj.radolfa.application.ports.out.LoadBestActiveDiscountPort;
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
import tj.radolfa.domain.model.Discount;
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
import java.math.RoundingMode;
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
    private final LoadBestActiveDiscountPort loadBestActiveDiscountPort;

    public CheckoutService(LoadCartPort loadCartPort,
                           SaveCartPort saveCartPort,
                           LoadSkuPort loadSkuPort,
                           LoadListingVariantPort loadListingVariantPort,
                           LoadProductBasePort loadProductBasePort,
                           LoadUserPort loadUserPort,
                           SaveOrderPort saveOrderPort,
                           StockAdjustmentPort stockAdjustmentPort,
                           LoyaltyCalculator loyaltyCalculator,
                           RedeemLoyaltyPointsUseCase redeemLoyaltyPointsUseCase,
                           LoadBestActiveDiscountPort loadBestActiveDiscountPort) {
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
        this.loadBestActiveDiscountPort = loadBestActiveDiscountPort;
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

        // 4. Compute subtotal using best-of (sale vs loyalty) per item
        LoyaltyProfile profile = user.loyalty();
        BigDecimal tierPct = loyaltyCalculator.resolveTierPercentage(profile); // 0 if no tier

        BigDecimal subtotalRaw = cart.getItems().stream()
                .map(item -> bestOfUnitPrice(item, tierPct).multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Money subtotal = new Money(subtotalRaw);

        // Tier discount is the difference between cart.total() (original snapshots) and best-of subtotal
        Money tierDiscount = new Money(cart.total().amount().subtract(subtotalRaw).max(BigDecimal.ZERO));
        Money afterTierDiscount = subtotal;

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
        BigDecimal totalRaw = afterTierDiscount.amount()
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

    /**
     * Returns the effective unit price for a cart item using best-of:
     * compares the active sale discount price vs the loyalty tier price
     * against the unit price snapshot, and returns whichever is lowest.
     */
    private BigDecimal bestOfUnitPrice(CartItem item, BigDecimal tierPct) {
        BigDecimal original = item.getUnitPriceSnapshot().amount();

        BigDecimal loyaltyPrice = tierPct.compareTo(BigDecimal.ZERO) > 0
                ? original.multiply(BigDecimal.ONE.subtract(
                        tierPct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                        .setScale(2, RoundingMode.HALF_UP)
                : original;

        Sku sku = loadSkuPort.findSkuById(item.getSkuId())
                .orElseThrow(() -> new IllegalStateException("SKU not found: " + item.getSkuId()));

        BigDecimal salePrice = loadBestActiveDiscountPort
                .findBestActiveForItemCode(sku.getSkuCode())
                .map(Discount::discountValue)
                .map(pct -> original.multiply(BigDecimal.ONE.subtract(
                        pct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                        .setScale(2, RoundingMode.HALF_UP))
                .orElse(original);

        return loyaltyPrice.min(salePrice);
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
