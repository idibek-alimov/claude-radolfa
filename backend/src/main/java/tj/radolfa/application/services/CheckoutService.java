package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import tj.radolfa.application.ports.in.discount.RecordDiscountApplicationUseCase;
import tj.radolfa.application.ports.in.discount.ResolveDiscountsUseCase;
import tj.radolfa.application.ports.in.loyalty.RedeemLoyaltyPointsUseCase;
import tj.radolfa.application.ports.in.order.CheckoutUseCase;
import tj.radolfa.application.ports.in.order.ExpireOrderUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadCartPort;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadPickpointPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveCartPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.application.ports.out.StockAdjustmentPort;
import tj.radolfa.domain.model.AppliedDiscount;
import tj.radolfa.domain.model.Cart;
import tj.radolfa.domain.model.CartItem;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.Pickpoint;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.Sku;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.service.LoyaltyCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CheckoutService implements CheckoutUseCase {

    private final LoadCartPort                     loadCartPort;
    private final SaveCartPort                     saveCartPort;
    private final LoadSkuPort                      loadSkuPort;
    private final LoadListingVariantPort           loadListingVariantPort;
    private final LoadProductBasePort              loadProductBasePort;
    private final LoadUserPort                     loadUserPort;
    private final SaveOrderPort                    saveOrderPort;
    private final StockAdjustmentPort              stockAdjustmentPort;
    private final LoyaltyCalculator                loyaltyCalculator;
    private final RedeemLoyaltyPointsUseCase       redeemLoyaltyPointsUseCase;
    private final ResolveDiscountsUseCase          resolveDiscountsUseCase;
    private final RecordDiscountApplicationUseCase recordDiscountApplicationUseCase;
    private final LoadPickpointPort                loadPickpointPort;
    private final LoadOrderPort                    loadOrderPort;
    private final ExpireOrderUseCase               expireOrderUseCase;

    @Value("${radolfa.payment.pending-timeout-minutes:30}")
    private int pendingTimeoutMinutes;

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
                           ResolveDiscountsUseCase resolveDiscountsUseCase,
                           RecordDiscountApplicationUseCase recordDiscountApplicationUseCase,
                           LoadPickpointPort loadPickpointPort,
                           LoadOrderPort loadOrderPort,
                           ExpireOrderUseCase expireOrderUseCase) {
        this.loadCartPort                    = loadCartPort;
        this.saveCartPort                    = saveCartPort;
        this.loadSkuPort                     = loadSkuPort;
        this.loadListingVariantPort          = loadListingVariantPort;
        this.loadProductBasePort             = loadProductBasePort;
        this.loadUserPort                    = loadUserPort;
        this.saveOrderPort                   = saveOrderPort;
        this.stockAdjustmentPort             = stockAdjustmentPort;
        this.loyaltyCalculator               = loyaltyCalculator;
        this.redeemLoyaltyPointsUseCase      = redeemLoyaltyPointsUseCase;
        this.resolveDiscountsUseCase         = resolveDiscountsUseCase;
        this.recordDiscountApplicationUseCase = recordDiscountApplicationUseCase;
        this.loadPickpointPort               = loadPickpointPort;
        this.loadOrderPort                   = loadOrderPort;
        this.expireOrderUseCase              = expireOrderUseCase;
    }

    @Override
    @Transactional
    public Result execute(Command command) {
        // 0. Validate delivery choice before touching any DB resources
        validateDelivery(command);

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

        // 2b. Pending-order guard — prevent double checkout within the payment window
        if (cart.getPendingOrderId() != null) {
            Order linked = loadOrderPort.loadById(cart.getPendingOrderId()).orElse(null);
            if (linked != null && linked.status() == OrderStatus.PENDING) {
                Instant cutoff = Instant.now().minus(pendingTimeoutMinutes, ChronoUnit.MINUTES);
                if (linked.createdAt().isAfter(cutoff)) {
                    throw new IllegalStateException(
                            "You already have a pending payment. Complete or cancel it before placing a new order.");
                }
                // Stale PENDING — auto-cancel and continue
                expireOrderUseCase.execute(linked.id(), "Payment window expired");
            } else {
                // Order already PAID/CANCELLED — clear the stale link and continue
                cart.unlinkOrder();
                saveCartPort.save(cart);
                // Reload to pick up fresh state
                cart = loadCartPort.findActiveByUserId(command.userId())
                        .orElseThrow(() -> new IllegalStateException("No active cart for user: " + command.userId()));
            }
        }

        // 3. Load all SKUs in one batch, then re-validate stock
        Set<Long> skuIds = cart.getItems().stream()
                .map(CartItem::getSkuId)
                .collect(Collectors.toSet());
        Map<Long, Sku> skuById = loadSkuPort.findAllByIdsAsMap(skuIds);

        for (CartItem item : cart.getItems()) {
            Sku sku = skuById.get(item.getSkuId());
            if (sku == null) throw new IllegalStateException("SKU not found: " + item.getSkuId());
            int available = sku.getStockQuantity() != null ? sku.getStockQuantity() : 0;
            if (available < item.getQuantity()) {
                throw new IllegalStateException(
                        "Insufficient stock for SKU " + item.getSkuId() +
                        " (available=" + available + ", requested=" + item.getQuantity() + ")");
            }
        }

        // 4. Resolve all applicable discounts once for the whole cart (batched, no N+1)
        List<String> itemCodes = skuById.values().stream()
                .map(Sku::getSkuCode)
                .distinct()
                .toList();
        Map<String, BigDecimal> priceByCode = skuById.values().stream()
                .filter(s -> s.getPrice() != null && s.getPrice().amount() != null)
                .collect(Collectors.toMap(Sku::getSkuCode, s -> s.getPrice().amount(), (a, b) -> a));
        Map<String, List<Discount>> resolvedDiscounts = resolveDiscountsUseCase.resolve(
                new ResolveDiscountsUseCase.Query(
                        itemCodes, command.userId(), cart.total().amount(),
                        cart.getCouponCode(), priceByCode));

        // 5. Compute per-line pricing: best-of (stacked sale vs loyalty)
        LoyaltyProfile profile = user.loyalty();
        BigDecimal tierPct = loyaltyCalculator.resolveTierPercentage(profile);

        List<LineResolution> lineResolutions = cart.getItems().stream()
                .map(item -> resolveLineResolution(item, tierPct, skuById, resolvedDiscounts))
                .toList();

        BigDecimal subtotalRaw = BigDecimal.ZERO;
        for (int i = 0; i < cart.getItems().size(); i++) {
            subtotalRaw = subtotalRaw.add(
                    lineResolutions.get(i).finalUnitPrice()
                            .multiply(BigDecimal.valueOf(cart.getItems().get(i).getQuantity())));
        }

        Money subtotal = new Money(subtotalRaw);
        Money tierDiscount = new Money(cart.total().amount().subtract(subtotalRaw).max(BigDecimal.ZERO));
        Money afterTierDiscount = subtotal;

        // 6. Apply points redemption
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

        // 7. Final total
        BigDecimal totalRaw = afterTierDiscount.amount()
                .subtract(pointsDiscount.amount())
                .max(BigDecimal.ZERO);
        Money total = new Money(totalRaw);

        // 7b. Batch-load variants and product bases (avoids N+1 in enrichToOrderItem)
        Set<Long> variantIds = skuById.values().stream()
                .map(Sku::getListingVariantId)
                .collect(Collectors.toSet());
        Map<Long, ListingVariant> variantById = loadListingVariantPort.findVariantsByIds(variantIds);

        Set<Long> productIds = variantById.values().stream()
                .map(ListingVariant::getProductBaseId)
                .collect(Collectors.toSet());
        Map<Long, ProductBase> productById = loadProductBasePort.findProductsByIds(productIds);

        // 8. Build order items
        List<OrderItem> orderItems = cart.getItems().stream()
                .map(item -> enrichToOrderItem(item, skuById, variantById, productById))
                .toList();

        // 9. Persist order
        Order newOrder = new Order.Builder()
                .userId(command.userId())
                .status(OrderStatus.PENDING)
                .totalAmount(total)
                .items(orderItems)
                .createdAt(Instant.now())
                .loyaltyPointsRedeemed(pointsToRedeem)
                .deliveryType(command.deliveryType())
                .deliveryAddress(command.address())
                .preferredTimeWindow(command.preferredTimeWindow())
                .pickpointId(command.pickpointId())
                .build();
        Order saved = saveOrderPort.save(newOrder);

        // 10. Record one discount_application row per stacked discount layer per line
        List<OrderItem> savedItems = saved.items();
        for (int i = 0; i < savedItems.size(); i++) {
            LineResolution lr = lineResolutions.get(i);
            if (lr.applied().isEmpty()) continue;
            OrderItem savedItem = savedItems.get(i);
            BigDecimal originalPrice = cart.getItems().get(i).getUnitPriceSnapshot().amount();
            for (AppliedDiscount ad : lr.applied()) {
                recordDiscountApplicationUseCase.execute(
                        new RecordDiscountApplicationUseCase.Command(
                                ad.discount().id(),
                                saved.id(),
                                savedItem.getId(),
                                savedItem.getSkuCode(),
                                savedItem.getQuantity(),
                                originalPrice,
                                ad.reducedUnitPrice(),
                                command.userId()
                        )
                );
            }
        }

        // 11. Decrement stock
        for (CartItem item : cart.getItems()) {
            stockAdjustmentPort.decrement(item.getSkuId(), item.getQuantity(),
                    saved.id(), command.userId());
        }

        // 12. Link cart to the pending order — cart stays ACTIVE until payment confirmed
        cart.linkOrder(saved.id());
        saveCartPort.save(cart);

        return new Result(saved.id(), subtotal, tierDiscount, pointsDiscount, total);
    }

    private void validateDelivery(Command command) {
        if (command.deliveryType() == null) {
            throw new IllegalArgumentException("deliveryType is required");
        }
        switch (command.deliveryType()) {
            case HOME -> {
                if (command.address() == null || command.address().isBlank()) {
                    throw new IllegalArgumentException("address is required for HOME delivery");
                }
            }
            case PICKPOINT -> {
                if (command.pickpointId() == null) {
                    throw new IllegalArgumentException("pickpointId is required for PICKPOINT delivery");
                }
                Pickpoint pickpoint = loadPickpointPort.findById(command.pickpointId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "pickpoint not found: " + command.pickpointId()));
                if (!pickpoint.active()) {
                    throw new IllegalArgumentException(
                            "pickpoint is not active: " + command.pickpointId());
                }
            }
        }
    }

    private record LineResolution(BigDecimal finalUnitPrice, List<AppliedDiscount> applied) {}

    /**
     * Returns the effective unit price and winning applied discounts.
     * Stacked discount wins only when the final stacked price ties or beats loyalty price
     * and strictly beats the original snapshot — loyalty alone is not recorded as a discount.
     */
    private LineResolution resolveLineResolution(CartItem item, BigDecimal tierPct,
                                                  Map<Long, Sku> skuById,
                                                  Map<String, List<Discount>> resolvedDiscounts) {
        BigDecimal original = item.getUnitPriceSnapshot().amount();

        BigDecimal loyaltyPrice = tierPct.compareTo(BigDecimal.ZERO) > 0
                ? original.multiply(BigDecimal.ONE.subtract(
                        tierPct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                        .setScale(2, RoundingMode.HALF_UP)
                : original;

        Sku sku = skuById.get(item.getSkuId());
        if (sku == null) throw new IllegalStateException("SKU not found: " + item.getSkuId());

        List<Discount> discounts = resolvedDiscounts.getOrDefault(sku.getSkuCode(), List.of());
        if (!discounts.isEmpty()) {
            List<AppliedDiscount> applied = AppliedDiscount.fold(discounts, original);
            BigDecimal stackedPrice = applied.get(applied.size() - 1).reducedUnitPrice();

            boolean discountWins = stackedPrice.compareTo(loyaltyPrice) <= 0
                    && stackedPrice.compareTo(original) < 0;

            if (discountWins) {
                return new LineResolution(stackedPrice, applied);
            }
        }

        return new LineResolution(loyaltyPrice.min(original), List.of());
    }

    private OrderItem enrichToOrderItem(CartItem cartItem,
                                         Map<Long, Sku> skuById,
                                         Map<Long, ListingVariant> variantById,
                                         Map<Long, ProductBase> productById) {
        Sku sku = skuById.get(cartItem.getSkuId());
        if (sku == null) throw new IllegalStateException("SKU not found: " + cartItem.getSkuId());

        ListingVariant variant = variantById.get(sku.getListingVariantId());
        if (variant == null) throw new IllegalStateException("Variant not found: " + sku.getListingVariantId());

        ProductBase product = productById.get(variant.getProductBaseId());
        if (product == null) throw new IllegalStateException("Product not found: " + variant.getProductBaseId());

        return new OrderItem(null, cartItem.getSkuId(), variant.getId(), sku.getSkuCode(),
                product.getName(), cartItem.getQuantity(), cartItem.getUnitPriceSnapshot());
    }
}
