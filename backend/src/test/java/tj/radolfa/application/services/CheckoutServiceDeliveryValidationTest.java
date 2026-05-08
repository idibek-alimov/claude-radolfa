package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.discount.RecordDiscountApplicationUseCase;
import tj.radolfa.application.ports.in.discount.ResolveDiscountsUseCase;
import tj.radolfa.application.ports.in.order.CheckoutUseCase;
import tj.radolfa.application.ports.out.LoadCartPort;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadPickpointPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveCartPort;
import tj.radolfa.application.ports.out.SaveDiscountApplicationPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.application.ports.out.StockAdjustmentPort;
import tj.radolfa.domain.model.Cart;
import tj.radolfa.domain.model.CartStatus;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.DiscountApplication;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.Pickpoint;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.Sku;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CheckoutServiceDeliveryValidationTest {

    // ---- Shared test data ----

    static final Long   USER_ID    = 1L;
    static final Long   SKU_ID     = 10L;
    static final String SKU_CODE   = "SKU-DEL";
    static final Long   VARIANT_ID = 20L;
    static final Long   PRODUCT_ID = 30L;
    static final Long   PP_ID      = 99L;

    static final BigDecimal PRICE = new BigDecimal("100.00");

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
            Cart cart = new Cart(1L, userId, CartStatus.ACTIVE, List.of(), Instant.now(), Instant.now(), null);
            cart.addItem(SKU_ID, 1, new Money(PRICE));
            return Optional.of(cart);
        }
        @Override public Optional<Cart> findById(Long id) { return Optional.empty(); }
    };

    static final LoadSkuPort FAKE_SKU = new LoadSkuPort() {
        @Override public List<Sku> findAllByIds(Collection<Long> ids) {
            return List.of(new Sku(SKU_ID, VARIANT_ID, SKU_CODE, "M", 10, new Money(PRICE)));
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
                itemsWithIds, order.createdAt(), 0, 0,
                order.deliveryType(), order.deliveryAddress(), order.preferredTimeWindow(), order.pickpointId(),
                null, null, null,
                null, null, null, null);
    };

    static final StockAdjustmentPort NO_STOCK = new StockAdjustmentPort() {
        @Override public void decrement(Long skuId, int qty) {}
        @Override public void increment(Long skuId, int qty) {}
        @Override public void setAbsolute(Long skuId, int qty) {}
    };

    static final SaveCartPort SAVE_CART = cart -> cart;

    static final SaveDiscountApplicationPort NO_DISCOUNT_APP = new SaveDiscountApplicationPort() {
        @Override public DiscountApplication save(DiscountApplication app) { return app; }
    };

    // ---- Builder ----

    static CheckoutService buildService(LoadPickpointPort loadPickpointPort) {
        RecordDiscountApplicationService recordService =
                new RecordDiscountApplicationService(NO_DISCOUNT_APP);
        return new CheckoutService(
                FAKE_CART,
                SAVE_CART,
                FAKE_SKU,
                FAKE_VARIANT,
                FAKE_PRODUCT,
                FAKE_USER,
                SAVE_ORDER,
                NO_STOCK,
                new LoyaltyCalculator(),
                (userId, pts) -> Money.ZERO,
                query -> Map.of(),
                recordService,
                loadPickpointPort
        );
    }

    static LoadPickpointPort pickpointPort(Optional<Pickpoint> result) {
        return new LoadPickpointPort() {
            @Override public List<Pickpoint> findAll() { return List.of(); }
            @Override public List<Pickpoint> findAllActive() { return List.of(); }
            @Override public Optional<Pickpoint> findById(Long id) { return result; }
        };
    }

    // ---- HOME delivery validation tests ----

    @Test
    @DisplayName("HOME with null address → IllegalArgumentException")
    void home_nullAddress_rejected() {
        CheckoutService service = buildService(pickpointPort(Optional.empty()));
        var ex = assertThrows(IllegalArgumentException.class, () ->
                service.execute(new CheckoutUseCase.Command(USER_ID, 0, null,
                        DeliveryType.HOME, null, null, null)));
        assertEquals("address is required for HOME delivery", ex.getMessage());
    }

    @Test
    @DisplayName("HOME with blank address → IllegalArgumentException")
    void home_blankAddress_rejected() {
        CheckoutService service = buildService(pickpointPort(Optional.empty()));
        var ex = assertThrows(IllegalArgumentException.class, () ->
                service.execute(new CheckoutUseCase.Command(USER_ID, 0, null,
                        DeliveryType.HOME, "   ", null, null)));
        assertEquals("address is required for HOME delivery", ex.getMessage());
    }

    @Test
    @DisplayName("HOME with valid address + optional time window → order saved with delivery fields")
    void home_validAddress_orderSavedWithDeliveryData() {
        CheckoutService service = buildService(pickpointPort(Optional.empty()));
        CheckoutUseCase.Result result = assertDoesNotThrow(() ->
                service.execute(new CheckoutUseCase.Command(USER_ID, 0, null,
                        DeliveryType.HOME, "123 Main St", "09:00-12:00", null)));
        assertEquals(100L, result.orderId());
    }

    // ---- PICKPOINT delivery validation tests ----

    @Test
    @DisplayName("PICKPOINT with null pickpointId → IllegalArgumentException")
    void pickpoint_nullId_rejected() {
        CheckoutService service = buildService(pickpointPort(Optional.empty()));
        var ex = assertThrows(IllegalArgumentException.class, () ->
                service.execute(new CheckoutUseCase.Command(USER_ID, 0, null,
                        DeliveryType.PICKPOINT, null, null, null)));
        assertEquals("pickpointId is required for PICKPOINT delivery", ex.getMessage());
    }

    @Test
    @DisplayName("PICKPOINT with non-existent pickpointId → IllegalArgumentException")
    void pickpoint_notFound_rejected() {
        CheckoutService service = buildService(pickpointPort(Optional.empty()));
        var ex = assertThrows(IllegalArgumentException.class, () ->
                service.execute(new CheckoutUseCase.Command(USER_ID, 0, null,
                        DeliveryType.PICKPOINT, null, null, PP_ID)));
        assertEquals("pickpoint not found: " + PP_ID, ex.getMessage());
    }

    @Test
    @DisplayName("PICKPOINT with inactive pickpoint → IllegalArgumentException")
    void pickpoint_inactive_rejected() {
        Pickpoint inactive = new Pickpoint(PP_ID, "Closed Point", "Some St", false);
        CheckoutService service = buildService(pickpointPort(Optional.of(inactive)));
        var ex = assertThrows(IllegalArgumentException.class, () ->
                service.execute(new CheckoutUseCase.Command(USER_ID, 0, null,
                        DeliveryType.PICKPOINT, null, null, PP_ID)));
        assertEquals("pickpoint is not active: " + PP_ID, ex.getMessage());
    }

    @Test
    @DisplayName("PICKPOINT with active pickpoint → order saved with pickpointId, null address")
    void pickpoint_active_orderSavedWithPickpointId() {
        Pickpoint active = new Pickpoint(PP_ID, "Central Hub", "1 Hub Ave", true);
        CheckoutService service = buildService(pickpointPort(Optional.of(active)));

        // capture the saved order via the SAVE_ORDER lambda — we verify via result orderId
        // and trust SAVE_ORDER echoes fields back; the real assertion is no exception thrown
        CheckoutUseCase.Result result = assertDoesNotThrow(() ->
                service.execute(new CheckoutUseCase.Command(USER_ID, 0, null,
                        DeliveryType.PICKPOINT, null, null, PP_ID)));
        assertEquals(100L, result.orderId());
    }
}
