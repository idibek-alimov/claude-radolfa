package tj.radolfa.infrastructure.persistence.spec;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tj.radolfa.application.readmodel.ListingQueryCriteria;
import tj.radolfa.application.readmodel.ListingSort;
import tj.radolfa.domain.model.ProductStatus;
import tj.radolfa.infrastructure.persistence.entity.BrandEntity;
import tj.radolfa.infrastructure.persistence.entity.CategoryEntity;
import tj.radolfa.infrastructure.persistence.entity.ColorEntity;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantEntity;
import tj.radolfa.infrastructure.persistence.entity.ProductBaseEntity;
import tj.radolfa.infrastructure.persistence.entity.ProductRatingSummaryEntity;
import tj.radolfa.infrastructure.persistence.entity.SkuEntity;
import tj.radolfa.infrastructure.persistence.repository.ListingVariantRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link ListingSpecifications#catalogFilter}, the SQL fallback
 * predicate/sort builder for {@code ListingReadAdapter#searchCatalog}.
 *
 * <p>Verifies the dynamic query directly against {@link ListingVariantRepository}
 * (not the full adapter, to avoid wiring {@code DiscountEnrichmentAdapter}): each
 * filter narrows results to the expected variants, each whitelisted sort orders
 * correctly, filters compose with AND, and {@code totalElements} is correct across
 * pages.
 */
// Run explicitly with: ./mvnw test -Dgroups=integration
// Requires Docker with API >= 1.44 (Docker Engine 29.x needs Testcontainers >= 1.21).
@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ListingCatalogSpecificationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    ListingVariantRepository variantRepo;

    @Autowired
    EntityManager em;

    private CategoryEntity bags;
    private CategoryEntity shoes;
    private BrandEntity acme;
    private BrandEntity zenith;
    private ColorEntity red;
    private ColorEntity blue;

    private Long alphaRedId;  // ACTIVE, Bags, Acme, red, price 100, stock 5
    private Long alphaBlueId; // ACTIVE, Bags, Acme, blue, price 150, stock 0 (out of stock)
    private Long betaRedId;   // ACTIVE, Shoes, Zenith, red, price 50, stock 10
    private Long gammaRedId;  // DRAFT (excluded), Bags, Acme, red

    @BeforeEach
    void setUp() {
        bags = persistCategory("Bags", "bags");
        shoes = persistCategory("Shoes", "shoes");
        acme = persistBrand("Acme");
        zenith = persistBrand("Zenith");
        red = persistColor("red", "Red", "#FF0000");
        blue = persistColor("blue", "Blue", "#0000FF");

        ProductBaseEntity alpha = persistProductBase("alpha", "Alpha Bag", bags, acme, ProductStatus.ACTIVE);
        ProductBaseEntity beta = persistProductBase("beta", "Beta Shoe", shoes, zenith, ProductStatus.ACTIVE);
        ProductBaseEntity gamma = persistProductBase("gamma", "Gamma Draft", bags, acme, ProductStatus.DRAFT);

        Instant base = Instant.now().minus(10, ChronoUnit.DAYS);

        ListingVariantEntity alphaRed = persistVariant(alpha, red, "alpha-red", "00001", base);
        persistSku(alphaRed, "SKU-00001-A", new BigDecimal("100.00"), 5);
        alphaRedId = alphaRed.getId();

        ListingVariantEntity alphaBlue = persistVariant(alpha, blue, "alpha-blue", "00002", base.plus(2, ChronoUnit.DAYS));
        persistSku(alphaBlue, "SKU-00002-A", new BigDecimal("150.00"), 0);
        alphaBlueId = alphaBlue.getId();

        ListingVariantEntity betaRed = persistVariant(beta, red, "beta-red", "00003", base.plus(1, ChronoUnit.DAYS));
        persistSku(betaRed, "SKU-00003-A", new BigDecimal("50.00"), 10);
        betaRedId = betaRed.getId();

        ListingVariantEntity gammaRed = persistVariant(gamma, red, "gamma-red", "00004", base.plus(3, ChronoUnit.DAYS));
        persistSku(gammaRed, "SKU-00004-A", new BigDecimal("10.00"), 10);
        gammaRedId = gammaRed.getId();

        persistRatingSummary(alphaRedId, new BigDecimal("4.50"), 10);
        persistRatingSummary(betaRedId, new BigDecimal("3.00"), 4);
        // alphaBlue intentionally has no rating summary row (coalesces to 0).

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("status filter: DRAFT products are always excluded")
    void excludesNonActiveProducts() {
        Page<ListingVariantEntity> result = search(ListingQueryCriteria.empty(), 1, 10);

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(ids(result)).doesNotContain(gammaRedId);
    }

    @Test
    @DisplayName("category filter narrows to the given category IDs")
    void categoryFilter() {
        ListingQueryCriteria criteria = criteriaWith(c -> c.categoryIds(List.of(bags.getId())));

        Page<ListingVariantEntity> result = search(criteria, 1, 10);

        assertThat(ids(result)).containsExactlyInAnyOrder(alphaRedId, alphaBlueId);
    }

    @Test
    @DisplayName("price range filter narrows by SKU min(originalPrice)")
    void priceRangeFilter() {
        ListingQueryCriteria criteria = criteriaWith(c -> c
                .priceMin(new BigDecimal("60"))
                .priceMax(new BigDecimal("200")));

        Page<ListingVariantEntity> result = search(criteria, 1, 10);

        assertThat(ids(result)).containsExactlyInAnyOrder(alphaRedId, alphaBlueId);
    }

    @Test
    @DisplayName("colour filter narrows to the given colour keys")
    void colorFilter() {
        ListingQueryCriteria criteria = criteriaWith(c -> c.colorKeys(List.of("red")));

        Page<ListingVariantEntity> result = search(criteria, 1, 10);

        assertThat(ids(result)).containsExactlyInAnyOrder(alphaRedId, betaRedId);
    }

    @Test
    @DisplayName("brand filter narrows to the given brand IDs")
    void brandFilter() {
        ListingQueryCriteria criteria = criteriaWith(c -> c.brandIds(List.of(zenith.getId())));

        Page<ListingVariantEntity> result = search(criteria, 1, 10);

        assertThat(ids(result)).containsExactly(betaRedId);
    }

    @Test
    @DisplayName("in-stock filter excludes variants with zero total SKU stock")
    void inStockFilter() {
        ListingQueryCriteria criteria = criteriaWith(c -> c.inStockOnly(true));

        Page<ListingVariantEntity> result = search(criteria, 1, 10);

        assertThat(ids(result)).containsExactlyInAnyOrder(alphaRedId, betaRedId);
    }

    @Test
    @DisplayName("combined category + colour filters compose with AND")
    void combinedFilters() {
        ListingQueryCriteria criteria = criteriaWith(c -> c
                .categoryIds(List.of(bags.getId()))
                .colorKeys(List.of("red")));

        Page<ListingVariantEntity> result = search(criteria, 1, 10);

        assertThat(ids(result)).containsExactly(alphaRedId);
    }

    @Test
    @DisplayName("CHEAPEST sort orders by ascending min(originalPrice)")
    void cheapestSort() {
        ListingQueryCriteria criteria = criteriaWith(c -> c.sort(ListingSort.CHEAPEST));

        Page<ListingVariantEntity> result = search(criteria, 1, 10);

        assertThat(ids(result)).containsExactly(betaRedId, alphaRedId, alphaBlueId);
    }

    @Test
    @DisplayName("DEAREST sort orders by descending min(originalPrice)")
    void dearestSort() {
        ListingQueryCriteria criteria = criteriaWith(c -> c.sort(ListingSort.DEAREST));

        Page<ListingVariantEntity> result = search(criteria, 1, 10);

        assertThat(ids(result)).containsExactly(alphaBlueId, alphaRedId, betaRedId);
    }

    @Test
    @DisplayName("NEWEST sort orders by descending createdAt")
    void newestSort() {
        ListingQueryCriteria criteria = criteriaWith(c -> c.sort(ListingSort.NEWEST));

        Page<ListingVariantEntity> result = search(criteria, 1, 10);

        // createdAt: alphaRed (oldest), betaRed (+1d), alphaBlue (+2d) — gamma excluded (DRAFT)
        assertThat(ids(result)).containsExactly(alphaBlueId, betaRedId, alphaRedId);
    }

    @Test
    @DisplayName("RATING sort orders by descending averageRating, missing summaries last")
    void ratingSort() {
        ListingQueryCriteria criteria = criteriaWith(c -> c.sort(ListingSort.RATING));

        Page<ListingVariantEntity> result = search(criteria, 1, 10);

        // alphaRed (4.50) > betaRed (3.00) > alphaBlue (no summary -> coalesced to 0)
        assertThat(ids(result)).containsExactly(alphaRedId, betaRedId, alphaBlueId);
    }

    @Test
    @DisplayName("totalElements and pagination are correct across pages")
    void pagination() {
        Page<ListingVariantEntity> page1 = search(ListingQueryCriteria.empty(), 1, 2);
        Page<ListingVariantEntity> page2 = search(ListingQueryCriteria.empty(), 2, 2);

        assertThat(page1.getTotalElements()).isEqualTo(3);
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page2.getContent()).hasSize(1);
        assertThat(ids(page1)).doesNotContainAnyElementsOf(ids(page2));
    }

    // ---- helpers --------------------------------------------------------

    private Page<ListingVariantEntity> search(ListingQueryCriteria criteria, int page, int size) {
        return variantRepo.findAll(ListingSpecifications.catalogFilter(criteria, List.of()),
                PageRequest.of(page - 1, size, Sort.unsorted()));
    }

    private List<Long> ids(Page<ListingVariantEntity> page) {
        return page.getContent().stream().map(ListingVariantEntity::getId).toList();
    }

    /** Builds a criteria record by mutating a default {@link CriteriaBuilderHelper}. */
    private ListingQueryCriteria criteriaWith(java.util.function.UnaryOperator<CriteriaBuilderHelper> customizer) {
        return customizer.apply(new CriteriaBuilderHelper()).build();
    }

    /** Tiny mutable builder over {@link ListingQueryCriteria} for readable test setup. */
    private static final class CriteriaBuilderHelper {
        private List<Long> categoryIds = List.of();
        private BigDecimal priceMin;
        private BigDecimal priceMax;
        private Integer minDiscountPercent;
        private List<String> colorKeys = List.of();
        private List<Long> brandIds = List.of();
        private Boolean inStockOnly;
        private ListingSort sort = ListingSort.POPULAR;

        CriteriaBuilderHelper categoryIds(List<Long> v) { this.categoryIds = v; return this; }
        CriteriaBuilderHelper priceMin(BigDecimal v) { this.priceMin = v; return this; }
        CriteriaBuilderHelper priceMax(BigDecimal v) { this.priceMax = v; return this; }
        CriteriaBuilderHelper colorKeys(List<String> v) { this.colorKeys = v; return this; }
        CriteriaBuilderHelper brandIds(List<Long> v) { this.brandIds = v; return this; }
        CriteriaBuilderHelper inStockOnly(boolean v) { this.inStockOnly = v; return this; }
        CriteriaBuilderHelper sort(ListingSort v) { this.sort = v; return this; }

        ListingQueryCriteria build() {
            return new ListingQueryCriteria(null, categoryIds, priceMin, priceMax, minDiscountPercent,
                    colorKeys, brandIds, inStockOnly, sort);
        }
    }

    // ---- persistence helpers --------------------------------------------

    private CategoryEntity persistCategory(String name, String slug) {
        CategoryEntity e = new CategoryEntity();
        e.setName(name);
        e.setSlug(slug);
        em.persist(e);
        return e;
    }

    private BrandEntity persistBrand(String name) {
        BrandEntity e = new BrandEntity();
        e.setName(name);
        em.persist(e);
        return e;
    }

    private ColorEntity persistColor(String key, String displayName, String hex) {
        ColorEntity e = new ColorEntity();
        e.setColorKey(key);
        e.setDisplayName(displayName);
        e.setHexCode(hex);
        em.persist(e);
        return e;
    }

    private ProductBaseEntity persistProductBase(String externalRef, String name, CategoryEntity category,
            BrandEntity brand, ProductStatus status) {
        ProductBaseEntity e = new ProductBaseEntity();
        e.setExternalRef(externalRef);
        e.setName(name);
        e.setCategory(category);
        e.setCategoryName(category.getName());
        e.setBrand(brand);
        e.setStatus(status);
        em.persist(e);
        return e;
    }

    private ListingVariantEntity persistVariant(ProductBaseEntity productBase, ColorEntity color, String slug,
            String productCode, Instant createdAt) {
        ListingVariantEntity e = new ListingVariantEntity();
        e.setProductBase(productBase);
        e.setColor(color);
        e.setSlug(slug);
        e.setProductCode(productCode);
        e.setWebDescription("Description for " + slug);
        e.setEnabled(true);
        e.setActive(true);
        e.setCreatedAt(createdAt);
        e.setUpdatedAt(createdAt);
        em.persist(e);
        return e;
    }

    private SkuEntity persistSku(ListingVariantEntity variant, String skuCode, BigDecimal price, int stock) {
        SkuEntity e = new SkuEntity();
        e.setListingVariant(variant);
        e.setSkuCode(skuCode);
        e.setSizeLabel("M");
        e.setStockQuantity(stock);
        e.setOriginalPrice(price);
        em.persist(e);
        return e;
    }

    private ProductRatingSummaryEntity persistRatingSummary(Long variantId, BigDecimal average, int reviewCount) {
        ProductRatingSummaryEntity e = new ProductRatingSummaryEntity();
        e.setListingVariantId(variantId);
        e.setAverageRating(average);
        e.setReviewCount(reviewCount);
        e.setLastCalculatedAt(Instant.now());
        em.persist(e);
        return e;
    }
}
