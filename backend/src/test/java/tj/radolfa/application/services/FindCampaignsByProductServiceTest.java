package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import tj.radolfa.application.ports.out.DiscountFilter;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.domain.model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class FindCampaignsByProductServiceTest {

    private FakeLoadListingVariantPort fakeVariants;
    private FakeLoadSkuPort fakeSkus;
    private FakeLoadDiscountPort fakeDiscounts;
    private FindCampaignsByProductService service;

    private static final DiscountType FLASH  = new DiscountType(1L, "FLASH_SALE", 1);
    private static final DiscountType SEASON = new DiscountType(2L, "SEASONAL", 3);
    private static final Instant FROM = Instant.parse("2024-01-01T00:00:00Z");
    private static final Instant UPTO = Instant.parse("2099-12-31T00:00:00Z");

    @BeforeEach
    void setUp() {
        fakeVariants  = new FakeLoadListingVariantPort();
        fakeSkus      = new FakeLoadSkuPort();
        fakeDiscounts = new FakeLoadDiscountPort();
        service       = new FindCampaignsByProductService(fakeVariants, fakeSkus, fakeDiscounts);
    }

    @Test
    @DisplayName("Product with no variants returns empty list")
    void execute_noVariants_returnsEmpty() {
        assertTrue(service.execute(99L).isEmpty());
    }

    @Test
    @DisplayName("Product with variants but no SKUs returns empty list")
    void execute_variantsNoSkus_returnsEmpty() {
        fakeVariants.store(99L, variant(10L));
        // no skus stored for variant 10
        assertTrue(service.execute(99L).isEmpty());
    }

    @Test
    @DisplayName("SKUs with no active discounts returns empty list")
    void execute_skusNoDiscounts_returnsEmpty() {
        fakeVariants.store(1L, variant(10L));
        fakeSkus.store(10L, sku(1L, 10L, "SKU-A"));
        // no discounts
        assertTrue(service.execute(1L).isEmpty());
    }

    @Test
    @DisplayName("Returns campaigns sorted by type rank ascending")
    void execute_multipleDiscounts_sortedByRank() {
        fakeVariants.store(1L, variant(10L));
        fakeSkus.store(10L, sku(1L, 10L, "SKU-A"), sku(2L, 10L, "SKU-B"));
        fakeDiscounts.store(
                discount(100L, SEASON, List.of("SKU-A")),  // rank 3
                discount(200L, FLASH,  List.of("SKU-B"))   // rank 1
        );

        List<DiscountSummary> result = service.execute(1L);

        assertEquals(2, result.size());
        assertEquals(200L, result.get(0).id(), "FLASH (rank 1) should be first");
        assertEquals(100L, result.get(1).id(), "SEASONAL (rank 3) should be second");
    }

    @Test
    @DisplayName("Same campaign covering multiple SKUs is returned only once (deduped)")
    void execute_sameCampaignOnMultipleSkus_deduped() {
        fakeVariants.store(1L, variant(10L));
        fakeSkus.store(10L, sku(1L, 10L, "SKU-A"), sku(2L, 10L, "SKU-B"));
        fakeDiscounts.store(discount(100L, FLASH, List.of("SKU-A", "SKU-B")));

        List<DiscountSummary> result = service.execute(1L);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).id());
    }

    @Test
    @DisplayName("Campaigns across multiple variants are merged and deduped")
    void execute_multipleVariants_mergedAndDeduped() {
        fakeVariants.store(1L, variant(10L), variant(20L));
        fakeSkus.store(10L, sku(1L, 10L, "SKU-A"));
        fakeSkus.store(20L, sku(2L, 20L, "SKU-B"));
        fakeDiscounts.store(
                discount(100L, FLASH, List.of("SKU-A", "SKU-B"))
        );

        List<DiscountSummary> result = service.execute(1L);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).id());
    }

    // ---- Helpers ----

    private static ListingVariant variant(Long id) {
        return new ListingVariant(id, 1L, "red", "slug-" + id, null,
                List.of(), List.of(), List.of(), null, "RD-" + id, true, true,
                null, null, null, null);
    }

    private static Sku sku(Long id, Long variantId, String skuCode) {
        return new Sku(id, variantId, skuCode, "M", 10,
                new Money(new BigDecimal("100.00")));
    }

    private static Discount discount(Long id, DiscountType type, List<String> codes) {
        List<DiscountTarget> targets = codes.stream().<DiscountTarget>map(SkuTarget::new).toList();
        return new Discount(id, type, targets, AmountType.PERCENT, new BigDecimal("10.00"),
                FROM, UPTO, false, "Camp-" + id, "#FFFFFF", null, null, null, null);
    }

    // ---- Fakes ----

    static class FakeLoadListingVariantPort implements LoadListingVariantPort {
        // productBaseId → list of variants
        private final Map<Long, List<ListingVariant>> store = new HashMap<>();

        void store(Long productBaseId, ListingVariant... variants) {
            store.put(productBaseId, new ArrayList<>(List.of(variants)));
        }

        @Override public Optional<ListingVariant> findVariantById(Long id) { return Optional.empty(); }
        @Override public Optional<ListingVariant> findByProductBaseIdAndColorKey(Long p, String c) { return Optional.empty(); }
        @Override public Optional<ListingVariant> findBySlug(String slug) { return Optional.empty(); }
        @Override public Map<Long, ListingVariant> findVariantsByIds(Collection<Long> ids) { return Map.of(); }

        @Override
        public List<ListingVariant> findAllByProductBaseId(Long productBaseId) {
            return store.getOrDefault(productBaseId, List.of());
        }
    }

    static class FakeLoadSkuPort implements LoadSkuPort {
        // variantId → list of skus
        private final Map<Long, List<Sku>> store = new HashMap<>();

        void store(Long variantId, Sku... skus) {
            store.put(variantId, new ArrayList<>(List.of(skus)));
        }

        @Override public Optional<Sku> findBySkuCode(String skuCode) { return Optional.empty(); }
        @Override public Optional<Sku> findSkuById(Long id) { return Optional.empty(); }
        @Override public List<Sku> findAllByIds(Collection<Long> ids) { return List.of(); }

        @Override
        public List<Sku> findSkusByVariantId(Long variantId) {
            return store.getOrDefault(variantId, List.of());
        }
    }

    static class FakeLoadDiscountPort implements LoadDiscountPort {
        private final List<Discount> discounts = new ArrayList<>();

        void store(Discount... items) {
            discounts.addAll(List.of(items));
        }

        @Override public Optional<Discount> findById(Long id) { return Optional.empty(); }
        @Override public List<Discount> findActiveByItemCode(String c) { return List.of(); }

        @Override
        public List<Discount> findActiveByItemCodes(Collection<String> itemCodes) {
            return discounts.stream()
                    .filter(d -> d.itemCodes().stream().anyMatch(itemCodes::contains))
                    .toList();
        }

        @Override
        public Page<Discount> findAll(DiscountFilter filter, Pageable pageable) {
            return new PageImpl<>(discounts, pageable, discounts.size());
        }
    }
}
