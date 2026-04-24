package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.discount.RecordDiscountApplicationUseCase;
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

class CheckoutServiceStackingTest {

    // ---- Shared test data ----

    static final Long USER_ID    = 1L;
    static final Long SKU_ID     = 10L;
    static final String SKU_CODE = "SKU-STACK";
    static final Long VARIANT_ID = 20L;
    static final Long PRODUCT_ID = 30L;

    static final BigDecimal ORIGINAL = new BigDecimal("100.00");

    // ---- Fakes ----

    static final LoadUserPort FAKE_USER = new LoadUserPort() {
        @Override public Optional<User> loadById(Long id) {
            return Optional.of(new User(id, new PhoneNumber("992000000000"),
                    UserRole.USER, "Test", null, LoyaltyProfile.empty(), true, 1L));
        }
        @Override public Optional<User> loadByPhone(String p) { return Optional.empty(); }
        @Override public List<User> findAllNonPermanent() { return List.of(); }
    };

    static final LoadCartPort FAKE_CART = new LoadCartPort() {
        @Override public Optional<Cart> findActiveByUserId(Long userId) {
            Cart cart = new Cart(1L, userId, CartStatus.ACTIVE, List.of(), Instant.now(), Instant.now());
            cart.addItem(SKU_ID, 1, new Money(ORIGINAL));
            return Optional.of(cart);
        }
        @Override public Optional<Cart> findById(Long id) { return Optional.empty(); }
    };

    static final LoadSkuPort FAKE_SKU = new LoadSkuPort() {
        @Override public List<Sku> findAllByIds(Collection<Long> ids) {
            return List.of(new Sku(SKU_ID, VARIANT_ID, SKU_CODE, "M", 10, new Money(ORIGINAL)));
        }
        @Override public Optional<Sku> findBySkuCode(String c) { return Optional.empty(); }
        @Override public Optional<Sku> findSkuById(Long id) { return Optional.empty(); }
        @Override public List<Sku> findSkusByVariantId(Long id) { return List.of(); }
    };

    static final LoadListingVariantPort FAKE_VARIANT = new LoadListingVariantPort() {
        @Override public Optional<ListingVariant> findVariantById(Long id) {
            return Optional.of(new ListingVariant(VARIANT_ID, PRODUCT_ID, "RED", "slug", null,
                    null, null, null, null, "RD-001", true, true, null, null, null, null));
        }
        @Override public Optional<ListingVariant> findByProductBaseIdAndColorKey(Long p, String c) { return Optional.empty(); }
        @Override public Optional<ListingVariant> findBySlug(String s) { return Optional.empty(); }
        @Override public List<ListingVariant> findAllByProductBaseId(Long id) { return List.of(); }
        @Override public Map<Long, ListingVariant> findVariantsByIds(Collection<Long> ids) { return Map.of(); }
    };

    static final LoadProductBasePort FAKE_PRODUCT = new LoadProductBasePort() {
        @Override public Optional<ProductBase> findById(Long id) {
            return Optional.of(new ProductBase(PRODUCT_ID, "EXT-001", "Test Product", null, null, null));
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
        @Override public void decrement(Long skuId, int qty) {}
        @Override public void increment(Long skuId, int qty) {}
        @Override public void setAbsolute(Long skuId, int qty) {}
    };

    // ---- Fixture helpers ----

    static Discount stackableDiscount(Long id, int rank, BigDecimal pct) {
        DiscountType type = new DiscountType((long) rank, "STACK-" + rank, rank, StackingPolicy.STACKABLE);
        return new Discount(id, type, List.of(new SkuTarget(SKU_CODE)),
                AmountType.PERCENT, pct,
                Instant.EPOCH, Instant.MAX, false, "Stack " + id, "#000",
                null, null, null, null);
    }

    CheckoutService buildService(Map<String, List<Discount>> resolvedMap,
                                  FakeSaveDiscountApplicationPort fakeAppPort) {
        RecordDiscountApplicationService recordService =
                new RecordDiscountApplicationService(fakeAppPort);
        return new CheckoutService(
                FAKE_CART,
                cart -> cart,
                FAKE_SKU,
                FAKE_VARIANT,
                FAKE_PRODUCT,
                FAKE_USER,
                SAVE_ORDER,
                NO_STOCK,
                new LoyaltyCalculator(),
                (userId, pts) -> Money.ZERO,
                query -> resolvedMap,
                recordService
        );
    }

    static class FakeSaveDiscountApplicationPort implements SaveDiscountApplicationPort {
        final List<DiscountApplication> stored = new ArrayList<>();
        @Override public DiscountApplication save(DiscountApplication app) {
            stored.add(app);
            return app;
        }
    }

    // ---- Tests ----

    @Test
    @DisplayName("Two STACKABLE discounts on the same SKU: checkout records 2 discount_application rows")
    void twoStackableDiscounts_recordTwoApplicationRows() {
        Discount s1 = stackableDiscount(1L, 1, new BigDecimal("20")); // 100 → 80
        Discount s2 = stackableDiscount(2L, 2, new BigDecimal("10")); // 80 → 72

        FakeSaveDiscountApplicationPort fakeAppPort = new FakeSaveDiscountApplicationPort();
        CheckoutService service = buildService(Map.of(SKU_CODE, List.of(s1, s2)), fakeAppPort);

        service.execute(new CheckoutUseCase.Command(USER_ID, 0, null));

        assertEquals(2, fakeAppPort.stored.size(), "One row per stacked discount layer");

        DiscountApplication first = fakeAppPort.stored.get(0);
        assertEquals(1L, first.discountId());
        assertEquals(new BigDecimal("80.00"), first.appliedUnitPrice());

        DiscountApplication second = fakeAppPort.stored.get(1);
        assertEquals(2L, second.discountId());
        assertEquals(new BigDecimal("72.00"), second.appliedUnitPrice());

        assertEquals(ORIGINAL, first.originalUnitPrice());
        assertEquals(ORIGINAL, second.originalUnitPrice());
    }

    @Test
    @DisplayName("No applicable discount: checkout records zero discount_application rows")
    void noDiscount_zeroApplicationRows() {
        FakeSaveDiscountApplicationPort fakeAppPort = new FakeSaveDiscountApplicationPort();
        CheckoutService service = buildService(Map.of(), fakeAppPort);

        service.execute(new CheckoutUseCase.Command(USER_ID, 0, null));

        assertEquals(0, fakeAppPort.stored.size());
    }
}
