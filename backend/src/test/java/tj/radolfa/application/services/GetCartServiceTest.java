package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.discount.ResolveDiscountsUseCase;
import tj.radolfa.application.ports.out.LoadCartPort;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.readmodel.CartView;
import tj.radolfa.domain.model.AmountType;
import tj.radolfa.domain.model.Cart;
import tj.radolfa.domain.model.CartStatus;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.DiscountType;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.LoyaltyTier;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.ProductStatus;
import tj.radolfa.domain.model.Sku;
import tj.radolfa.domain.model.SkuTarget;
import tj.radolfa.domain.model.StackingPolicy;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;
import tj.radolfa.domain.model.WinningMechanism;
import tj.radolfa.domain.service.CartLinePricer;
import tj.radolfa.domain.service.LoyaltyCalculator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@code GetCartService} computes per-line display pricing and the
 * partitioned summary via {@link CartLinePricer} — the same best-of decision
 * {@code CheckoutService} uses to charge — and that the resolver query mirrors
 * checkout's exactly (see {@code CheckoutServiceStackingTest} for the charge-side
 * parity proof).
 *
 * <p>Note: a cart belongs to one user, so {@code tierPct} is constant across all
 * of its lines. That makes a single cart containing all three mechanisms
 * (CAMPAIGN, LOYALTY, and NONE) domain-impossible — at {@code tierPct > 0} every
 * line is at least LOYALTY-eligible, so NONE only occurs at {@code tierPct == 0}.
 * The mixed-cart test below therefore covers CAMPAIGN + LOYALTY together (the
 * partitioning logic), and the guest test separately covers NONE.
 */
class GetCartServiceTest {

    static final Long USER_ID = 1L;

    static final Long   SKU_A_ID     = 10L;
    static final String SKU_A_CODE   = "SKU-A";
    static final Long   VARIANT_A_ID = 20L;
    static final Long   PRODUCT_A_ID = 30L;

    static final Long   SKU_B_ID     = 11L;
    static final String SKU_B_CODE   = "SKU-B";
    static final Long   VARIANT_B_ID = 21L;
    static final Long   PRODUCT_B_ID = 31L;

    // ---- Fixture builders ----

    static Sku sku(Long id, Long variantId, String code, BigDecimal price) {
        return new Sku(id, variantId, code, "M", 10, new Money(price));
    }

    static ListingVariant variant(Long id, Long productBaseId, String colorKey) {
        return new ListingVariant(id, productBaseId, colorKey, "slug-" + id, null,
                List.of("img.png"), null, null, null, "PC-" + id, true, true, null, null, null, null);
    }

    static ProductBase product(Long id, String name, String category) {
        return new ProductBase(id, "EXT-" + id, name, category, null, null, ProductStatus.ACTIVE, null);
    }

    static User userWithTier(BigDecimal tierPct) {
        LoyaltyProfile loyalty;
        if (tierPct == null || tierPct.compareTo(BigDecimal.ZERO) == 0) {
            loyalty = LoyaltyProfile.empty();
        } else {
            LoyaltyTier tier = new LoyaltyTier(1L, "Gold", tierPct, null, null, 1, "#FFD700");
            loyalty = new LoyaltyProfile(tier, 0, null, null, null, false, null);
        }
        return new User(USER_ID, new PhoneNumber("992000000000"), UserRole.USER,
                "Test", null, loyalty, true, 1L);
    }

    static Discount percentDiscount(String skuCode, BigDecimal pct) {
        DiscountType type = new DiscountType(1L, "SALE", 1, StackingPolicy.BEST_WINS);
        return new Discount(99L, type, List.of(new SkuTarget(skuCode)),
                AmountType.PERCENT, pct,
                Instant.EPOCH, Instant.MAX, false, "Sale", "#F00",
                null, null, null, null);
    }

    static LoadSkuPort fakeSkuPort(Map<Long, Sku> byId) {
        return new LoadSkuPort() {
            @Override public List<Sku> findAllByIds(Collection<Long> ids) {
                return ids.stream().map(byId::get).filter(s -> s != null).toList();
            }
            @Override public Optional<Sku> findBySkuCode(String c) { return Optional.empty(); }
            @Override public Optional<Sku> findSkuById(Long id) { return Optional.ofNullable(byId.get(id)); }
            @Override public List<Sku> findSkusByVariantId(Long id) { return List.of(); }
        };
    }

    static LoadListingVariantPort fakeVariantPort(Map<Long, ListingVariant> byId) {
        return new LoadListingVariantPort() {
            @Override public Optional<ListingVariant> findVariantById(Long id) { return Optional.ofNullable(byId.get(id)); }
            @Override public Optional<ListingVariant> findByProductBaseIdAndColorKey(Long p, String c) { return Optional.empty(); }
            @Override public Optional<ListingVariant> findBySlug(String s) { return Optional.empty(); }
            @Override public Optional<ListingVariant> findByProductCode(String code) { return Optional.empty(); }
            @Override public List<ListingVariant> findAllByProductBaseId(Long id) { return List.of(); }
            @Override public Map<Long, ListingVariant> findVariantsByIds(Collection<Long> ids) {
                return ids.stream().filter(byId::containsKey)
                        .collect(java.util.stream.Collectors.toMap(i -> i, byId::get));
            }
        };
    }

    static LoadProductBasePort fakeProductPort(Map<Long, ProductBase> byId) {
        return new LoadProductBasePort() {
            @Override public Optional<ProductBase> findById(Long id) { return Optional.ofNullable(byId.get(id)); }
            @Override public Optional<ProductBase> findByExternalRef(String ref) { return Optional.empty(); }
            @Override public Map<Long, ProductBase> findProductsByIds(Collection<Long> ids) {
                return ids.stream().filter(byId::containsKey)
                        .collect(java.util.stream.Collectors.toMap(i -> i, byId::get));
            }
        };
    }

    static LoadUserPort fakeUserPort(User user) {
        return new LoadUserPort() {
            @Override public Optional<User> loadByPhone(String phone) { return Optional.empty(); }
            @Override public Optional<User> loadById(Long id) { return Optional.of(user); }
            @Override public List<User> findAllNonPermanent() { return List.of(); }
            @Override public List<User> findByRoleAndEnabledTrue(UserRole role) { return List.of(); }
        };
    }

    /** Captures the last query it received, for assertion. */
    static class CapturingResolver implements ResolveDiscountsUseCase {
        ResolveDiscountsUseCase.Query lastQuery;
        final Map<String, List<Discount>> result;
        CapturingResolver(Map<String, List<Discount>> result) { this.result = result; }
        @Override public Map<String, List<Discount>> resolve(Query q) {
            this.lastQuery = q;
            return result;
        }
    }

    GetCartService buildService(LoadCartPort cartPort, Map<Long, Sku> skus, Map<Long, ListingVariant> variants,
                                 Map<Long, ProductBase> products, User user, ResolveDiscountsUseCase resolver) {
        return new GetCartService(
                cartPort,
                fakeSkuPort(skus),
                fakeVariantPort(variants),
                fakeProductPort(products),
                fakeUserPort(user),
                new LoyaltyCalculator(),
                resolver,
                new CartLinePricer()
        );
    }

    static LoadCartPort cartPort(Cart cart) {
        return new LoadCartPort() {
            @Override public Optional<Cart> findActiveByUserId(Long userId) { return Optional.ofNullable(cart); }
            @Override public Optional<Cart> findById(Long id) { return Optional.empty(); }
        };
    }

    // ---- Tests ----

    @Test
    @DisplayName("Mixed cart: CAMPAIGN beats loyalty on one line, loyalty beats campaign on another — partitions correctly")
    void mixedCart_partitionsCorrectly() {
        Cart cart = new Cart(100L, USER_ID, CartStatus.ACTIVE, List.of(), Instant.now(), Instant.now(), null);
        cart.addItem(SKU_A_ID, 1, new Money(new BigDecimal("100.00")));
        cart.addItem(SKU_B_ID, 1, new Money(new BigDecimal("200.00")));

        Map<Long, Sku> skus = Map.of(
                SKU_A_ID, sku(SKU_A_ID, VARIANT_A_ID, SKU_A_CODE, new BigDecimal("100.00")),
                SKU_B_ID, sku(SKU_B_ID, VARIANT_B_ID, SKU_B_CODE, new BigDecimal("200.00")));
        Map<Long, ListingVariant> variants = Map.of(
                VARIANT_A_ID, variant(VARIANT_A_ID, PRODUCT_A_ID, "RED"),
                VARIANT_B_ID, variant(VARIANT_B_ID, PRODUCT_B_ID, "BLUE"));
        Map<Long, ProductBase> products = Map.of(
                PRODUCT_A_ID, product(PRODUCT_A_ID, "Tote", "Bags"),
                PRODUCT_B_ID, product(PRODUCT_B_ID, "Watch", "Watches"));

        // tierPct=10%: loyaltyPrice(A)=90, loyaltyPrice(B)=180.
        // A: 20% campaign -> stacked=80 <= 90 -> CAMPAIGN wins.
        // B: 5% campaign  -> stacked=190 > 180 -> LOYALTY wins.
        Map<String, List<Discount>> resolved = Map.of(
                SKU_A_CODE, List.of(percentDiscount(SKU_A_CODE, new BigDecimal("20"))),
                SKU_B_CODE, List.of(percentDiscount(SKU_B_CODE, new BigDecimal("5"))));

        GetCartService service = buildService(cartPort(cart), skus, variants, products,
                userWithTier(new BigDecimal("10")), q -> resolved);

        CartView view = service.execute(USER_ID);

        CartView.ItemView lineA = view.items().stream().filter(i -> i.skuId().equals(SKU_A_ID)).findFirst().orElseThrow();
        CartView.ItemView lineB = view.items().stream().filter(i -> i.skuId().equals(SKU_B_ID)).findFirst().orElseThrow();

        assertEquals(WinningMechanism.CAMPAIGN, lineA.mechanism());
        assertEquals(20, lineA.discountPercent());
        assertEquals(new BigDecimal("80.00"), lineA.unitPrice().amount());
        assertEquals("Bags", lineA.category());

        assertEquals(WinningMechanism.LOYALTY, lineB.mechanism());
        assertEquals(10, lineB.discountPercent());
        assertEquals(new BigDecimal("180.00"), lineB.unitPrice().amount());

        CartView.Summary summary = view.summary();
        assertEquals(new BigDecimal("300.00"), summary.subtotal().amount());
        assertEquals(new BigDecimal("260.00"), summary.total().amount());
        assertEquals(new BigDecimal("20.00"), summary.itemDiscounts().amount());
        assertEquals(new BigDecimal("20.00"), summary.crownTier().amount());
        assertEquals(new BigDecimal("40.00"), summary.savings().amount());

        // Invariant: subtotal - total == itemDiscounts + crownTier, exactly.
        BigDecimal lhs = summary.subtotal().amount().subtract(summary.total().amount());
        BigDecimal rhs = summary.itemDiscounts().amount().add(summary.crownTier().amount());
        assertEquals(0, lhs.compareTo(rhs));

        // The cart's discounted total must equal what checkout would actually charge.
        assertEquals(0, view.total().amount().compareTo(summary.total().amount()));
    }

    @Test
    @DisplayName("Guest / no tier, no campaign: mechanism is NONE, discountPercent is null, total == subtotal")
    void guestNoTierNoCampaign_mechanismNone() {
        Cart cart = new Cart(101L, USER_ID, CartStatus.ACTIVE, List.of(), Instant.now(), Instant.now(), null);
        cart.addItem(SKU_A_ID, 2, new Money(new BigDecimal("50.00")));

        Map<Long, Sku> skus = Map.of(SKU_A_ID, sku(SKU_A_ID, VARIANT_A_ID, SKU_A_CODE, new BigDecimal("50.00")));
        Map<Long, ListingVariant> variants = Map.of(VARIANT_A_ID, variant(VARIANT_A_ID, PRODUCT_A_ID, "RED"));
        Map<Long, ProductBase> products = Map.of(PRODUCT_A_ID, product(PRODUCT_A_ID, "Tote", "Bags"));

        GetCartService service = buildService(cartPort(cart), skus, variants, products,
                userWithTier(BigDecimal.ZERO), q -> Map.of());

        CartView view = service.execute(USER_ID);
        CartView.ItemView line = view.items().get(0);

        assertEquals(WinningMechanism.NONE, line.mechanism());
        assertNull(line.discountPercent());
        assertEquals(0, view.summary().subtotal().amount().compareTo(view.summary().total().amount()));
        assertEquals(0, view.summary().itemDiscounts().amount().compareTo(BigDecimal.ZERO));
        assertEquals(0, view.summary().crownTier().amount().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Coupon code on the cart flows unchanged into the ResolveDiscountsUseCase query")
    void couponCode_passedThroughToResolverQuery() {
        Cart cart = new Cart(102L, USER_ID, CartStatus.ACTIVE, List.of(), Instant.now(), Instant.now(), "PROMO1");
        cart.addItem(SKU_A_ID, 1, new Money(new BigDecimal("100.00")));

        Map<Long, Sku> skus = Map.of(SKU_A_ID, sku(SKU_A_ID, VARIANT_A_ID, SKU_A_CODE, new BigDecimal("100.00")));
        Map<Long, ListingVariant> variants = Map.of(VARIANT_A_ID, variant(VARIANT_A_ID, PRODUCT_A_ID, "RED"));
        Map<Long, ProductBase> products = Map.of(PRODUCT_A_ID, product(PRODUCT_A_ID, "Tote", "Bags"));

        CapturingResolver resolver = new CapturingResolver(Map.of());
        GetCartService service = buildService(cartPort(cart), skus, variants, products,
                userWithTier(BigDecimal.ZERO), resolver);

        service.execute(USER_ID);

        assertEquals("PROMO1", resolver.lastQuery.couponCode());
        assertEquals(USER_ID, resolver.lastQuery.userId());
        assertEquals(List.of(SKU_A_CODE), resolver.lastQuery.itemCodes());
        assertEquals(0, resolver.lastQuery.cartSubtotal().compareTo(new BigDecimal("100.00")));
    }

    @Test
    @DisplayName("No active cart at all -> CartView.empty()")
    void noActiveCart_returnsEmptyView() {
        GetCartService service = buildService(cartPort(null), Map.of(), Map.of(), Map.of(),
                userWithTier(BigDecimal.ZERO), q -> Map.of());

        CartView view = service.execute(USER_ID);

        assertEquals(CartView.empty(), view);
    }

    @Test
    @DisplayName("Active cart with zero items -> empty items + Summary.empty(), but cartId/coupon preserved")
    void activeCartZeroItems_emptySummaryButIdentityPreserved() {
        Cart cart = new Cart(103L, USER_ID, CartStatus.ACTIVE, List.of(), Instant.now(), Instant.now(), "KEEP");

        GetCartService service = buildService(cartPort(cart), Map.of(), Map.of(), Map.of(),
                userWithTier(BigDecimal.ZERO), q -> Map.of());

        CartView view = service.execute(USER_ID);

        assertEquals(103L, view.cartId());
        assertEquals("KEEP", view.couponCode());
        assertTrue(view.items().isEmpty());
        assertEquals(CartView.Summary.empty(), view.summary());
    }
}
