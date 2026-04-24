package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.discount.ResolveDiscountsUseCase;
import tj.radolfa.application.ports.in.order.CheckoutUseCase;
import tj.radolfa.application.ports.out.LoadCartPort;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveDiscountApplicationPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.application.ports.out.StockAdjustmentPort;
import tj.radolfa.domain.model.AmountType;
import tj.radolfa.domain.model.Cart;
import tj.radolfa.domain.model.CartStatus;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.DiscountApplication;
import tj.radolfa.domain.model.DiscountType;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.LoyaltyTier;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.Sku;
import tj.radolfa.domain.model.SkuTarget;
import tj.radolfa.domain.model.StackingPolicy;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;
import tj.radolfa.domain.service.LoyaltyCalculator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the loyalty-wins guard: when the stacked discount price is worse than the
 * loyalty price, the loyalty price applies and no discount_application row is recorded.
 */
class ResolveDiscountsServiceLoyaltyGuardTest {

    static final Long USER_ID    = 1L;
    static final Long SKU_ID     = 10L;
    static final String SKU_CODE = "SKU-GUARD";
    static final Long VARIANT_ID = 20L;
    static final Long PRODUCT_ID = 30L;

    static final BigDecimal ORIGINAL    = new BigDecimal("100.00");
    // Loyalty tier: 25% off → loyalty price = 75.00
    static final BigDecimal LOYALTY_PCT = new BigDecimal("25");

    // ---- Fakes ----

    static LoadUserPort fakeUserWithLoyalty(BigDecimal loyaltyPct) {
        return new LoadUserPort() {
            @Override public Optional<User> loadById(Long id) {
                LoyaltyTier tier = new LoyaltyTier(1L, "Silver", loyaltyPct, null, null, 1, "silver");
                LoyaltyProfile loyalty = new LoyaltyProfile(tier, 0, null, null, null, false, null);
                return Optional.of(new User(id, new PhoneNumber("992000000001"),
                        UserRole.USER, "Guard", null, loyalty, true, 1L));
            }
            @Override public Optional<User> loadByPhone(String p) { return Optional.empty(); }
            @Override public List<User> findAllNonPermanent() { return List.of(); }
        };
    }

    static final LoadCartPort FAKE_CART = new LoadCartPort() {
        @Override public Optional<Cart> findActiveByUserId(Long userId) {
            Cart cart = new Cart(1L, userId, CartStatus.ACTIVE, List.of(), Instant.now(), Instant.now(), null);
            cart.addItem(SKU_ID, 1, new Money(ORIGINAL));
            return Optional.of(cart);
        }
        @Override public Optional<Cart> findById(Long id) { return Optional.empty(); }
    };

    static final LoadSkuPort FAKE_SKU = new LoadSkuPort() {
        @Override public List<Sku> findAllByIds(Collection<Long> ids) {
            return List.of(new Sku(SKU_ID, VARIANT_ID, SKU_CODE, "M", 5, new Money(ORIGINAL)));
        }
        @Override public Optional<Sku> findBySkuCode(String c) { return Optional.empty(); }
        @Override public Optional<Sku> findSkuById(Long id) { return Optional.empty(); }
        @Override public List<Sku> findSkusByVariantId(Long id) { return List.of(); }
    };

    static final LoadListingVariantPort FAKE_VARIANT = new LoadListingVariantPort() {
        @Override public Optional<ListingVariant> findVariantById(Long id) {
            return Optional.of(new ListingVariant(VARIANT_ID, PRODUCT_ID, "BLUE", "slug",
                    null, null, null, null, null, "RD-002", true, true, null, null, null, null));
        }
        @Override public Optional<ListingVariant> findByProductBaseIdAndColorKey(Long p, String c) { return Optional.empty(); }
        @Override public Optional<ListingVariant> findBySlug(String s) { return Optional.empty(); }
        @Override public List<ListingVariant> findAllByProductBaseId(Long id) { return List.of(); }
        @Override public Map<Long, ListingVariant> findVariantsByIds(Collection<Long> ids) { return Map.of(); }
    };

    static final LoadProductBasePort FAKE_PRODUCT = new LoadProductBasePort() {
        @Override public Optional<ProductBase> findById(Long id) {
            return Optional.of(new ProductBase(PRODUCT_ID, "EXT-002", "Guard Product", null, null, null));
        }
        @Override public Optional<ProductBase> findByExternalRef(String ref) { return Optional.empty(); }
    };

    static final SaveOrderPort SAVE_ORDER = order -> {
        List<OrderItem> itemsWithIds = order.items().stream()
                .map(i -> new OrderItem(200L, i.getSkuId(), i.getListingVariantId(),
                        i.getSkuCode(), i.getProductName(), i.getQuantity(), i.getPrice()))
                .toList();
        return new Order(100L, order.userId(), null, OrderStatus.PENDING, order.totalAmount(),
                itemsWithIds, order.createdAt(), 0, 0);
    };

    static final StockAdjustmentPort NO_STOCK = new StockAdjustmentPort() {
        @Override public void decrement(Long id, int qty) {}
        @Override public void increment(Long id, int qty) {}
        @Override public void setAbsolute(Long id, int qty) {}
    };

    static class FakeSaveDiscountApplicationPort implements SaveDiscountApplicationPort {
        final List<DiscountApplication> stored = new ArrayList<>();
        @Override public DiscountApplication save(DiscountApplication app) {
            stored.add(app);
            return app;
        }
    }

    CheckoutService buildService(BigDecimal loyaltyPct,
                                  Map<String, List<Discount>> resolvedMap,
                                  FakeSaveDiscountApplicationPort fakeAppPort) {
        RecordDiscountApplicationService recordService =
                new RecordDiscountApplicationService(fakeAppPort);
        return new CheckoutService(
                FAKE_CART, cart -> cart, FAKE_SKU, FAKE_VARIANT, FAKE_PRODUCT,
                fakeUserWithLoyalty(loyaltyPct),
                SAVE_ORDER, NO_STOCK, new LoyaltyCalculator(),
                (userId, pts) -> Money.ZERO,
                query -> resolvedMap,
                recordService
        );
    }

    Discount saleDiscount(BigDecimal salePct) {
        DiscountType type = new DiscountType(1L, "SALE", 1, StackingPolicy.BEST_WINS);
        return new Discount(99L, type,
                List.of(new SkuTarget(SKU_CODE)),
                AmountType.PERCENT, salePct,
                Instant.EPOCH, Instant.MAX, false, "Sale", "#F00",
                null, null, null, null);
    }

    // ---- Tests ----

    @Test
    @DisplayName("Stacked price (80) > loyalty price (75): loyalty wins, zero discount_application rows")
    void stackedPriceWorseThanLoyalty_loyaltyWins_noApplicationRows() {
        // Loyalty: 25% off → 75.00. Sale: 20% off → 80.00. Loyalty wins.
        Discount d = saleDiscount(new BigDecimal("20"));
        FakeSaveDiscountApplicationPort fakeAppPort = new FakeSaveDiscountApplicationPort();
        CheckoutService service = buildService(LOYALTY_PCT, Map.of(SKU_CODE, List.of(d)), fakeAppPort);

        service.execute(new CheckoutUseCase.Command(USER_ID, 0, null));

        assertEquals(0, fakeAppPort.stored.size(),
                "Loyalty (75) beats stacked sale (80): no discount rows expected");
    }

    @Test
    @DisplayName("Stacked price (70) < loyalty price (75): sale wins, one discount_application row")
    void stackedPriceBetterThanLoyalty_saleWins_oneApplicationRow() {
        // Loyalty: 25% off → 75.00. Sale: 30% off → 70.00. Sale wins.
        Discount d = saleDiscount(new BigDecimal("30"));
        FakeSaveDiscountApplicationPort fakeAppPort = new FakeSaveDiscountApplicationPort();
        CheckoutService service = buildService(LOYALTY_PCT, Map.of(SKU_CODE, List.of(d)), fakeAppPort);

        service.execute(new CheckoutUseCase.Command(USER_ID, 0, null));

        assertEquals(1, fakeAppPort.stored.size(),
                "Sale (70) beats loyalty (75): one discount row expected");
        assertEquals(new BigDecimal("70.00"), fakeAppPort.stored.get(0).appliedUnitPrice());
    }
}
