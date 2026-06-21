package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.discount.RecordDiscountApplicationUseCase;
import tj.radolfa.application.ports.in.loyalty.AwardLoyaltyPointsUseCase;
import tj.radolfa.application.ports.in.order.CheckoutUseCase;
import tj.radolfa.application.ports.out.LoadCartPort;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadPickpointPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.application.ports.out.SaveCartPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.application.ports.out.StockAdjustmentPort;
import tj.radolfa.domain.model.Cart;
import tj.radolfa.domain.model.CartStatus;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PaymentMethod;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.Pickpoint;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.ProductStatus;
import tj.radolfa.domain.model.Sku;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;
import tj.radolfa.domain.service.CartLinePricer;
import tj.radolfa.domain.service.LoyaltyCalculator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2: verifies the COD branch places the order directly into {@code AWAITING_COD}
 * with the handling fee added, finalizes the cart, awards loyalty points, and sends
 * exactly one confirmation notification — while CARD keeps today's PENDING/link flow
 * untouched and sends no notification.
 */
class CheckoutServiceCodTest {

    static final Long   USER_ID    = 1L;
    static final Long   SKU_ID     = 10L;
    static final String SKU_CODE   = "SKU-COD";
    static final Long   VARIANT_ID = 20L;
    static final Long   PRODUCT_ID = 30L;

    static final BigDecimal PRICE      = new BigDecimal("100.00");
    static final BigDecimal HANDLING_FEE = new BigDecimal("15");

    // ---- Fakes ----

    static final LoadUserPort FAKE_USER = new LoadUserPort() {
        @Override public Optional<User> loadById(Long id) {
            return Optional.of(new User(id, new PhoneNumber("992000000000"),
                    UserRole.USER, "Test", null, LoyaltyProfile.empty(), true, 1L));
        }
        @Override public Optional<User> loadByPhone(String p) { return Optional.empty(); }
        @Override public List<User> findAllNonPermanent() { return List.of(); }
        @Override public List<User> findByRoleAndEnabledTrue(UserRole r) { return List.of(); }
    };

    static Cart freshCart() {
        Cart cart = new Cart(1L, USER_ID, CartStatus.ACTIVE, List.of(), Instant.now(), Instant.now(), null);
        cart.addItem(SKU_ID, 1, new Money(PRICE));
        return cart;
    }

    static final LoadSkuPort FAKE_SKU = new LoadSkuPort() {
        @Override public List<Sku> findAllByIds(Collection<Long> ids) {
            return List.of(new Sku(SKU_ID, VARIANT_ID, SKU_CODE, "M", 10, new Money(PRICE)));
        }
        @Override public Optional<Sku> findBySkuCode(String c) { return Optional.empty(); }
        @Override public Optional<Sku> findSkuById(Long id) { return Optional.empty(); }
        @Override public List<Sku> findSkusByVariantId(Long id) { return List.of(); }
    };

    static final ListingVariant FAKE_VARIANT_OBJ = new ListingVariant(VARIANT_ID, PRODUCT_ID, "RED", "slug", null,
            null, null, null, null, "00003", true, true, null, null, null, null);
    static final ProductBase FAKE_PRODUCT_OBJ = new ProductBase(PRODUCT_ID, "EXT-003", "COD Product", null, null, null, ProductStatus.DRAFT, null);

    static final LoadListingVariantPort FAKE_VARIANT = new LoadListingVariantPort() {
        @Override public Optional<ListingVariant> findVariantById(Long id) { return Optional.of(FAKE_VARIANT_OBJ); }
        @Override public Optional<ListingVariant> findByProductBaseIdAndColorKey(Long p, String c) { return Optional.empty(); }
        @Override public Optional<ListingVariant> findBySlug(String s) { return Optional.empty(); }
        @Override public Optional<ListingVariant> findByProductCode(String code) { return Optional.empty(); }
        @Override public List<ListingVariant> findAllByProductBaseId(Long id) { return List.of(); }
        @Override public Map<Long, ListingVariant> findVariantsByIds(Collection<Long> ids) {
            return ids.contains(VARIANT_ID) ? Map.of(VARIANT_ID, FAKE_VARIANT_OBJ) : Map.of();
        }
    };

    static final LoadProductBasePort FAKE_PRODUCT = new LoadProductBasePort() {
        @Override public Optional<ProductBase> findById(Long id) { return Optional.of(FAKE_PRODUCT_OBJ); }
        @Override public Optional<ProductBase> findByExternalRef(String ref) { return Optional.empty(); }
        @Override public Map<Long, ProductBase> findProductsByIds(Collection<Long> ids) {
            return ids.contains(PRODUCT_ID) ? Map.of(PRODUCT_ID, FAKE_PRODUCT_OBJ) : Map.of();
        }
    };

    /** Preserves status/paymentMethod/handlingFee from the builder, unlike the other tests' fixed-PENDING fake. */
    static final SaveOrderPort SAVE_ORDER = order -> {
        List<OrderItem> itemsWithIds = order.items().stream()
                .map(i -> new OrderItem(200L, i.getSkuId(), i.getListingVariantId(),
                        i.getSkuCode(), i.getProductName(), i.getQuantity(), i.getPrice(), 0, null, null, i.getSellerId()))
                .toList();
        return new Order.Builder()
                .id(100L).userId(order.userId()).status(order.status())
                .totalAmount(order.totalAmount()).items(itemsWithIds).createdAt(order.createdAt())
                .deliveryType(order.deliveryType()).deliveryAddress(order.deliveryAddress())
                .preferredTimeWindow(order.preferredTimeWindow()).pickpointId(order.pickpointId())
                .paymentMethod(order.paymentMethod()).handlingFee(order.handlingFee())
                .build();
    };

    static final StockAdjustmentPort NO_STOCK = new StockAdjustmentPort() {
        @Override public void decrement(Long skuId, int qty) {}
        @Override public void increment(Long skuId, int qty) {}
        @Override public void setAbsolute(Long skuId, int qty) {}
    };

    static final LoadPickpointPort FAKE_LOAD_PICKPOINT = new LoadPickpointPort() {
        @Override public List<Pickpoint> findAll(String search) { return List.of(); }
        @Override public List<Pickpoint> findAllActive() { return List.of(); }
        @Override public Optional<Pickpoint> findById(Long id) { return Optional.empty(); }
    };

    static final LoadOrderPort FAKE_LOAD_ORDER = new LoadOrderPort() {
        @Override public List<Order> loadByUserId(Long id) { return List.of(); }
        @Override public Optional<Order> loadById(Long id) { return Optional.empty(); }
        @Override public Optional<Order> loadByExternalOrderId(String s) { return Optional.empty(); }
        @Override public List<Order> loadRecentPaidByUserId(Long id, int limit) { return List.of(); }
    };

    static class CapturingAwardLoyaltyPoints implements AwardLoyaltyPointsUseCase {
        final List<Long> orderIdsAwarded = new ArrayList<>();
        @Override public void execute(Long userId, Long orderId) { orderIdsAwarded.add(orderId); }
    }

    static class CapturingNotificationPort implements NotificationPort {
        final List<Long> confirmedOrderIds = new ArrayList<>();
        @Override public void sendOrderConfirmation(Long userId, Long orderId) { confirmedOrderIds.add(orderId); }
        @Override public void sendOrderStatusUpdate(Long userId, Long orderId, OrderStatus newStatus) {}
        @Override public void sendReviewApprovedNotification(Long userId, Long reviewId) {}
        @Override public void sendReviewReplyNotification(Long userId, Long reviewId) {}
        @Override public void sendDeliveryCode(Long userId, Long orderId, String code, Instant expiresAt) {}
        @Override public void sendPickpointExpiryWarning(Long userId, Long orderId, int daysRemaining) {}
        @Override public void sendPickpointOrderExpiredCancellation(Long userId, Long orderId) {}
    }

    static class CapturingSaveCartPort implements SaveCartPort {
        Cart lastSaved;
        @Override public Cart save(Cart cart) { lastSaved = cart; return cart; }
    }

    /** Test rig bundling the captors needed by the assertions below. */
    record Rig(CheckoutService service, CapturingAwardLoyaltyPoints loyalty,
               CapturingNotificationPort notifications, CapturingSaveCartPort savedCart) {}

    static Rig buildRig(BigDecimal codHandlingFee) {
        LoadCartPort fakeCart = new LoadCartPort() {
            @Override public Optional<Cart> findActiveByUserId(Long userId) { return Optional.of(freshCart()); }
            @Override public Optional<Cart> findById(Long id) { return Optional.empty(); }
        };
        CapturingSaveCartPort saveCart = new CapturingSaveCartPort();
        CapturingAwardLoyaltyPoints loyalty = new CapturingAwardLoyaltyPoints();
        CapturingNotificationPort notificationPort = new CapturingNotificationPort();
        OrderNotificationService notificationService = new OrderNotificationService(notificationPort);
        RecordDiscountApplicationUseCase noOpRecordDiscount = command -> { };

        CheckoutService service = new CheckoutService(
                fakeCart,
                saveCart,
                FAKE_SKU,
                FAKE_VARIANT,
                FAKE_PRODUCT,
                FAKE_USER,
                SAVE_ORDER,
                NO_STOCK,
                new LoyaltyCalculator(),
                new CartLinePricer(),
                (userId, pts) -> Money.ZERO,
                query -> Map.of(),
                noOpRecordDiscount,
                FAKE_LOAD_PICKPOINT,
                FAKE_LOAD_ORDER,
                (orderId, reason) -> { },
                loyalty,
                notificationService,
                codHandlingFee
        );
        return new Rig(service, loyalty, notificationPort, saveCart);
    }

    // ---- Tests ----

    @Test
    @DisplayName("COD: order placed directly as AWAITING_COD with the handling fee added, cart finalized, loyalty awarded, one notification sent")
    void cod_placesAwaitingCodOrderWithFeeAndFinalizesCart() {
        Rig rig = buildRig(HANDLING_FEE);

        CheckoutUseCase.Result result = rig.service().execute(new CheckoutUseCase.Command(
                USER_ID, 0, null, DeliveryType.HOME, "123 Test St", null, null, PaymentMethod.COD));

        assertEquals(OrderStatus.AWAITING_COD, result.status());
        assertEquals(PaymentMethod.COD, result.paymentMethod());
        assertEquals(HANDLING_FEE, result.handlingFee().amount());
        assertEquals(PRICE.add(HANDLING_FEE), result.total().amount());

        assertEquals(CartStatus.CHECKED_OUT, rig.savedCart().lastSaved.getStatus(), "cart must be finalized, not merely linked");
        assertNull(rig.savedCart().lastSaved.getPendingOrderId(), "checkout() clears pendingOrderId");

        assertEquals(List.of(100L), rig.loyalty().orderIdsAwarded, "loyalty must be awarded exactly once for the new order");
        assertEquals(List.of(100L), rig.notifications().confirmedOrderIds, "exactly one confirmation notification must be sent");
    }

    @Test
    @DisplayName("CARD: order stays PENDING with zero handling fee, cart linked (not finalized), no notification sent")
    void card_keepsExistingPendingFlowAndSendsNoNotification() {
        Rig rig = buildRig(HANDLING_FEE);

        CheckoutUseCase.Result result = rig.service().execute(new CheckoutUseCase.Command(
                USER_ID, 0, null, DeliveryType.HOME, "123 Test St", null, null, PaymentMethod.CARD));

        assertEquals(OrderStatus.PENDING, result.status());
        assertEquals(PaymentMethod.CARD, result.paymentMethod());
        assertEquals(BigDecimal.ZERO, result.handlingFee().amount());
        assertEquals(PRICE, result.total().amount());

        assertEquals(CartStatus.ACTIVE, rig.savedCart().lastSaved.getStatus(), "CARD cart stays ACTIVE pending payment");
        assertEquals(100L, rig.savedCart().lastSaved.getPendingOrderId());

        assertTrue(rig.loyalty().orderIdsAwarded.isEmpty(), "loyalty award is saga-only for CARD");
        assertTrue(rig.notifications().confirmedOrderIds.isEmpty(), "notification is saga-only for CARD");
    }

    @Test
    @DisplayName("Omitted paymentMethod defaults to CARD")
    void omittedPaymentMethod_defaultsToCard() {
        Rig rig = buildRig(HANDLING_FEE);

        CheckoutUseCase.Result result = rig.service().execute(new CheckoutUseCase.Command(
                USER_ID, 0, null, DeliveryType.HOME, "123 Test St", null, null, null));

        assertEquals(OrderStatus.PENDING, result.status());
        assertEquals(PaymentMethod.CARD, result.paymentMethod());
        assertEquals(BigDecimal.ZERO, result.handlingFee().amount());
    }
}
