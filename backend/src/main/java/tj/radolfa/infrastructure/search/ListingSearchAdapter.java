package tj.radolfa.infrastructure.search;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.AggregationsContainer;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.ListingIndexPort;
import tj.radolfa.application.ports.out.SearchListingPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.infrastructure.persistence.adapter.DiscountEnrichmentAdapter;
import tj.radolfa.infrastructure.persistence.adapter.DiscountEnrichmentAdapter.DiscountInfo;
import tj.radolfa.infrastructure.persistence.entity.ColorEntity;
import tj.radolfa.infrastructure.persistence.entity.ProductRatingSummaryEntity;
import tj.radolfa.infrastructure.persistence.repository.ColorRepository;
import tj.radolfa.infrastructure.persistence.repository.ListingVariantRepository;
import tj.radolfa.infrastructure.persistence.repository.ProductBaseRepository;
import tj.radolfa.infrastructure.persistence.repository.ProductRatingSummaryRepository;
import tj.radolfa.infrastructure.persistence.repository.SellerRepository;
import tj.radolfa.infrastructure.persistence.repository.SkuRepository;
import tj.radolfa.application.readmodel.CatalogFacets;
import tj.radolfa.application.readmodel.CatalogFacets.BrandFacet;
import tj.radolfa.application.readmodel.CatalogFacets.ColorFacet;
import tj.radolfa.application.readmodel.CatalogFacets.DiscountFacets;
import tj.radolfa.application.readmodel.CatalogFacets.PriceFacet;
import tj.radolfa.application.readmodel.CatalogResult;
import tj.radolfa.application.readmodel.ListingQueryCriteria;
import tj.radolfa.application.readmodel.ListingSort;
import tj.radolfa.application.readmodel.ListingVariantDto;
import tj.radolfa.application.readmodel.ListingVariantDto.TagView;
import tj.radolfa.application.readmodel.SkuDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Real Elasticsearch adapter for the listings index.
 *
 * <p>
 * Handles both indexing (write) and search (read).
 * Index/delete are fire-and-forget: failures are logged but never
 * propagate to the sync pipeline.
 *
 * <p>Search results are enriched with discount and SKU data from the DB.
 */
@Component
@Profile("!test")
public class ListingSearchAdapter implements ListingIndexPort, SearchListingPort {

        private static final Logger LOG = LoggerFactory.getLogger(ListingSearchAdapter.class);

        private final ListingSearchRepository repository;
        private final ElasticsearchOperations operations;
        private final DiscountEnrichmentAdapter discountEnrichment;
        private final SkuRepository skuRepo;
        private final ListingVariantRepository variantRepo;
        private final ProductRatingSummaryRepository ratingRepo;
        private final ProductBaseRepository productBaseRepo;
        private final SellerRepository sellerRepo;
        private final ColorRepository colorRepo;

        public ListingSearchAdapter(ListingSearchRepository repository,
                        ElasticsearchOperations operations,
                        DiscountEnrichmentAdapter discountEnrichment,
                        SkuRepository skuRepo,
                        ListingVariantRepository variantRepo,
                        ProductRatingSummaryRepository ratingRepo,
                        ProductBaseRepository productBaseRepo,
                        SellerRepository sellerRepo,
                        ColorRepository colorRepo) {
                this.repository = repository;
                this.operations = operations;
                this.discountEnrichment = discountEnrichment;
                this.skuRepo = skuRepo;
                this.variantRepo = variantRepo;
                this.ratingRepo = ratingRepo;
                this.productBaseRepo = productBaseRepo;
                this.sellerRepo = sellerRepo;
                this.colorRepo = colorRepo;
        }

        // ---- ListingIndexPort (write) ----

        @Override
        public void index(Long variantId, Long productBaseId, String slug, String name, String category,
                        String colorKey, String colorHexCode,
                        String description, List<String> images,
                        Double price, Integer totalStock,
                        Instant lastSyncAt,
                        String productCode, List<String> skuCodes,
                        String status,
                        Long categoryId, Long brandId, String brandName,
                        Integer discountPercentage, Double ratingAverage,
                        Instant createdAt) {
                try {
                        ListingDocument doc = new ListingDocument(
                                        variantId, slug, name, category,
                                        colorKey, colorHexCode, description,
                                        images, price, totalStock,
                                        lastSyncAt,
                                        productCode,
                                        skuCodes != null ? skuCodes : List.of(),
                                        productBaseId, status,
                                        categoryId, brandId, brandName,
                                        discountPercentage, ratingAverage, createdAt);
                        repository.save(doc);
                        LOG.debug("Indexed listing variant id={}, slug={}", variantId, slug);
                } catch (Exception e) {
                        LOG.warn("Failed to index listing variant id={}: {}", variantId, e.getMessage());
                }
        }

        @Override
        public void delete(String slug) {
                try {
                        repository.deleteBySlug(slug);
                        LOG.debug("Deleted listing from index slug={}", slug);
                } catch (Exception e) {
                        LOG.warn("Failed to delete listing from index slug={}: {}", slug, e.getMessage());
                }
        }

        // ---- SearchListingPort (read) ----

        @Override
        public PageResult<ListingVariantDto> search(String query, int page, int limit) {
                Query combined = Query.of(q -> q.bool(b -> b
                                .must(fuzzyMultiMatchQuery(query))
                                .filter(activeStatusFilter())));

                NativeQuery searchQuery = NativeQuery.builder()
                                .withQuery(combined)
                                .withPageable(PageRequest.of(page - 1, limit))
                                .build();

                SearchHits<ListingDocument> hits = operations.search(searchQuery, ListingDocument.class);

                List<ListingVariantDto> enriched = enrichHits(hits);

                long totalHits = hits.getTotalHits();
                boolean last = (long) page * limit >= totalHits;

                return new PageResult<>(enriched, totalHits, page, limit, last);
        }

        @Override
        public CatalogResult searchCatalog(ListingQueryCriteria criteria, int page, int limit) {
                Query combined = buildCatalogQuery(criteria);
                List<SortOptions> sort = sortOptions(criteria.sort(), criteria.hasQuery());

                NativeQuery searchQuery = NativeQuery.builder()
                                .withQuery(combined)
                                .withSort(sort)
                                .withPageable(PageRequest.of(page - 1, limit))
                                .withAggregation("brands", brandAggregation())
                                .withAggregation("colors", colorAggregation())
                                .withAggregation("price_min", metricAggregation("min"))
                                .withAggregation("price_max", metricAggregation("max"))
                                .withAggregation("disc_any", discountAnyAggregation())
                                .withAggregation("disc_30", discountAtLeastAggregation(30))
                                .withAggregation("disc_50", discountAtLeastAggregation(50))
                                .build();

                SearchHits<ListingDocument> hits = operations.search(searchQuery, ListingDocument.class);

                List<ListingVariantDto> enriched = enrichHits(hits);

                long totalHits = hits.getTotalHits();
                boolean last = (long) page * limit >= totalHits;
                PageResult<ListingVariantDto> pageResult = new PageResult<>(enriched, totalHits, page, limit, last);

                CatalogFacets facets = buildFacets(hits.getAggregations());

                return new CatalogResult(pageResult, facets);
        }

        // ---- Catalog query / sort / facets ----

        /**
         * The fuzzy multi-field {@code should} query used for free-text search,
         * boosted toward exact product-code/SKU-code matches.
         */
        private Query fuzzyMultiMatchQuery(String query) {
                String upperQuery = query != null ? query.toUpperCase() : "";
                return BoolQuery.of(b -> b
                                .should(
                                                Query.of(q -> q.match(m -> m
                                                                .field("name")
                                                                .query(query)
                                                                .fuzziness("AUTO")
                                                                .boost(3.0f))),
                                                Query.of(q -> q.match(m -> m
                                                                .field("webDescription")
                                                                .query(query)
                                                                .fuzziness("AUTO"))),
                                                Query.of(q -> q.match(m -> m
                                                                .field("colorKey")
                                                                .query(query)
                                                                .boost(2.0f))),
                                                // Product code prefix search (e.g. "RD-100")
                                                Query.of(q -> q.wildcard(w -> w
                                                                .field("productCode")
                                                                .wildcard("*" + upperQuery + "*")
                                                                .caseInsensitive(true)
                                                                .boost(4.0f))),
                                                // SKU code prefix search (e.g. "RD-10047-S")
                                                Query.of(q -> q.wildcard(w -> w
                                                                .field("skuCodes")
                                                                .wildcard("*" + upperQuery + "*")
                                                                .caseInsensitive(true)
                                                                .boost(5.0f))))
                                .minimumShouldMatch("1"))._toQuery();
        }

        private Query activeStatusFilter() {
                return Query.of(q -> q.term(t -> t.field("status").value("ACTIVE")));
        }

        /**
         * Builds the {@code bool} query for {@link #searchCatalog}: free-text
         * (or {@code match_all} when browsing) plus structured {@code filter}
         * clauses derived from {@link ListingQueryCriteria}. Filters are
         * term/range queries only — no raw user input ever reaches the query DSL.
         */
        private Query buildCatalogQuery(ListingQueryCriteria criteria) {
                BoolQuery.Builder bool = new BoolQuery.Builder();

                if (criteria.hasQuery()) {
                        bool.must(fuzzyMultiMatchQuery(criteria.query()));
                } else {
                        bool.must(Query.of(q -> q.matchAll(m -> m)));
                }

                bool.filter(activeStatusFilter());

                if (criteria.hasCategoryFilter()) {
                        bool.filter(Query.of(q -> q.terms(t -> t
                                        .field("categoryId")
                                        .terms(tf -> tf.value(criteria.categoryIds().stream()
                                                        .map(id -> FieldValue.of(id.longValue()))
                                                        .toList())))));
                }

                if (criteria.priceMin() != null || criteria.priceMax() != null) {
                        bool.filter(Query.of(q -> q.range(r -> r.number(n -> {
                                n.field("price");
                                if (criteria.priceMin() != null) n.gte(criteria.priceMin().doubleValue());
                                if (criteria.priceMax() != null) n.lte(criteria.priceMax().doubleValue());
                                return n;
                        }))));
                }

                if (!criteria.colorKeys().isEmpty()) {
                        bool.filter(Query.of(q -> q.terms(t -> t
                                        .field("colorKey")
                                        .terms(tf -> tf.value(criteria.colorKeys().stream()
                                                        .map(FieldValue::of)
                                                        .toList())))));
                }

                if (!criteria.brandIds().isEmpty()) {
                        bool.filter(Query.of(q -> q.terms(t -> t
                                        .field("brandId")
                                        .terms(tf -> tf.value(criteria.brandIds().stream()
                                                        .map(id -> FieldValue.of(id.longValue()))
                                                        .toList())))));
                }

                if (criteria.minDiscountPercent() != null && criteria.minDiscountPercent() > 0) {
                        bool.filter(Query.of(q -> q.range(r -> r.number(n -> n
                                        .field("discountPercentage")
                                        .gte(criteria.minDiscountPercent().doubleValue())))));
                }

                if (Boolean.TRUE.equals(criteria.inStockOnly())) {
                        bool.filter(Query.of(q -> q.range(r -> r.number(n -> n
                                        .field("totalStock")
                                        .gt(0.0)))));
                }

                return Query.of(q -> q.bool(bool.build()));
        }

        /**
         * Maps the whitelisted {@link ListingSort} to a concrete Elasticsearch sort.
         * Raw client input never reaches this method — only enum constants.
         */
        private List<SortOptions> sortOptions(ListingSort sort, boolean hasQuery) {
                return switch (sort) {
                        // _id has no doc values in Elasticsearch (not sortable); "_doc" (index
                        // order) approximates the variant-id-ascending default closely enough
                        // since the entity reindex writes documents in id order.
                        case POPULAR -> hasQuery
                                        ? List.of(SortOptions.of(s -> s.score(sc -> sc.order(SortOrder.Desc))))
                                        : List.of(SortOptions.of(s -> s.field(f -> f.field("_doc").order(SortOrder.Asc))));
                        case CHEAPEST -> List.of(SortOptions.of(s -> s.field(f -> f.field("price").order(SortOrder.Asc))));
                        case DEAREST -> List.of(SortOptions.of(s -> s.field(f -> f.field("price").order(SortOrder.Desc))));
                        case RATING -> List.of(SortOptions.of(s -> s.field(f -> f.field("ratingAverage").order(SortOrder.Desc))));
                        case NEWEST -> List.of(SortOptions.of(s -> s.field(f -> f.field("createdAt").order(SortOrder.Desc))));
                        case BIGGEST_DISCOUNT -> List.of(SortOptions.of(s -> s.field(f -> f.field("discountPercentage").order(SortOrder.Desc))));
                };
        }

        private co.elastic.clients.elasticsearch._types.aggregations.Aggregation brandAggregation() {
                return co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> a
                                .terms(t -> t.field("brandId").size(50))
                                .aggregations("brandName", sub -> sub.terms(t -> t.field("brandName").size(1))));
        }

        private co.elastic.clients.elasticsearch._types.aggregations.Aggregation colorAggregation() {
                return co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> a
                                .terms(t -> t.field("colorKey").size(50)));
        }

        private co.elastic.clients.elasticsearch._types.aggregations.Aggregation metricAggregation(String type) {
                return co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> "min".equals(type)
                                ? a.min(m -> m.field("price"))
                                : a.max(m -> m.field("price")));
        }

        private co.elastic.clients.elasticsearch._types.aggregations.Aggregation discountAnyAggregation() {
                return co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> a
                                .filter(f -> f.range(r -> r.number(n -> n.field("discountPercentage").gt(0.0)))));
        }

        private co.elastic.clients.elasticsearch._types.aggregations.Aggregation discountAtLeastAggregation(int minPercent) {
                return co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> a
                                .filter(f -> f.range(r -> r.number(n -> n.field("discountPercentage").gte((double) minPercent)))));
        }

        /**
         * Reads back the {@code brands}/{@code colors}/{@code price_*}/{@code disc_*}
         * aggregations into the sidebar facet model. Colour display name + hex are
         * resolved from {@link ColorRepository} (not stored per-document).
         */
        private CatalogFacets buildFacets(AggregationsContainer<?> aggregationsContainer) {
                if (!(aggregationsContainer instanceof ElasticsearchAggregations aggregations)) {
                        return CatalogFacets.empty();
                }
                Map<String, Aggregate> aggs = aggregations.aggregationsAsMap().entrySet().stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().aggregation().getAggregate()));

                List<BrandFacet> brands = List.of();
                Aggregate brandAgg = aggs.get("brands");
                if (brandAgg != null && brandAgg.isLterms()) {
                        brands = brandAgg.lterms().buckets().array().stream()
                                        .map(bucket -> {
                                                String name = subTermsKey(bucket, "brandName");
                                                return new BrandFacet(bucket.key(), name, bucket.docCount());
                                        })
                                        .toList();
                }

                List<ColorFacet> colors = List.of();
                Aggregate colorAgg = aggs.get("colors");
                if (colorAgg != null && colorAgg.isSterms()) {
                        Map<String, ColorEntity> colorByKey = colorRepo.findAll().stream()
                                        .collect(Collectors.toMap(ColorEntity::getColorKey, c -> c, (a, b) -> a));
                        colors = colorAgg.sterms().buckets().array().stream()
                                        .map(bucket -> {
                                                String key = bucket.key().stringValue();
                                                ColorEntity color = colorByKey.get(key);
                                                String name = color != null ? color.getDisplayName() : key;
                                                String hex = color != null ? color.getHexCode() : null;
                                                return new ColorFacet(key, name, hex, bucket.docCount());
                                        })
                                        .toList();
                }

                BigDecimal priceMin = metricValue(aggs.get("price_min"));
                BigDecimal priceMax = metricValue(aggs.get("price_max"));

                long discAny = filterDocCount(aggs.get("disc_any"));
                long disc30 = filterDocCount(aggs.get("disc_30"));
                long disc50 = filterDocCount(aggs.get("disc_50"));

                return new CatalogFacets(brands, colors, new PriceFacet(priceMin, priceMax),
                                new DiscountFacets(discAny, disc30, disc50));
        }

        private String subTermsKey(LongTermsBucket bucket, String subAggName) {
                Aggregate sub = bucket.aggregations().get(subAggName);
                if (sub == null || !sub.isSterms()) return null;
                List<StringTermsBucket> buckets = sub.sterms().buckets().array();
                return buckets.isEmpty() ? null : buckets.get(0).key().stringValue();
        }

        private BigDecimal metricValue(Aggregate aggregate) {
                if (aggregate == null) return null;
                Double value = aggregate.isMin() ? aggregate.min().value()
                                : aggregate.isMax() ? aggregate.max().value()
                                : null;
                if (value == null || value.isNaN() || value.isInfinite()) return null;
                return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
        }

        private long filterDocCount(Aggregate aggregate) {
                return aggregate != null && aggregate.isFilter() ? aggregate.filter().docCount() : 0L;
        }

        /**
         * Runs the discount/SKU/tag/rating/seller batch-enrichment shared by
         * {@link #search} and {@link #searchCatalog}.
         */
        private List<ListingVariantDto> enrichHits(SearchHits<ListingDocument> hits) {
                List<ListingVariantDto> items = hits.getSearchHits().stream()
                                .map(SearchHit::getContent)
                                .map(this::toDto)
                                .toList();

                // Enrich with discounts from the discounts table
                List<Long> variantIds = items.stream()
                                .map(ListingVariantDto::variantId)
                                .toList();
                Map<Long, DiscountInfo> discountMap = discountEnrichment.resolveForVariants(variantIds);

                // Batch-load SKUs, tags, ratings, and seller names from DB
                Map<Long, List<SkuDto>> skuMap = loadSkuMap(variantIds);
                Map<Long, List<TagView>> tagMap = loadTagMap(variantIds);
                Map<Long, ProductRatingSummaryEntity> ratingMap = loadRatingMap(variantIds);

                List<Long> productBaseIds = items.stream()
                                .map(ListingVariantDto::productBaseId)
                                .distinct()
                                .toList();
                Map<Long, String> sellerMap = loadSellerMap(productBaseIds);

                return items.stream()
                                .map(dto -> {
                                        DiscountInfo discount = discountMap.get(dto.variantId());
                                        BigDecimal originalPrice = discount != null
                                                        ? discount.originalPrice()
                                                        : dto.originalPrice();
                                        BigDecimal discountPrice = discount != null
                                                        ? discount.discountedPrice() : null;
                                        Integer discountPercentage = discount != null
                                                        ? discount.discountPercentage().intValue() : null;
                                        String discountName = discount != null ? discount.saleTitle() : null;
                                        String discountColorHex = discount != null ? discount.saleColorHex() : null;
                                        boolean isPartialDiscount = discount != null && discount.isPartialDiscount();
                                        // Loyalty isn't known yet (enriched by TierPricingEnricher post-query); the
                                        // only active mechanism visible here is the campaign, if any.
                                        String winningSource = discountPrice != null ? "CAMPAIGN" : null;
                                        List<SkuDto> skus = skuMap.getOrDefault(dto.variantId(), List.of());
                                        List<TagView> tags = tagMap.getOrDefault(dto.variantId(), List.of());
                                        ProductRatingSummaryEntity rating = ratingMap.get(dto.variantId());
                                        return new ListingVariantDto(
                                                        dto.productBaseId(), dto.variantId(), dto.slug(), dto.colorDisplayName(),
                                                        dto.categoryName(), dto.colorKey(), dto.colorHex(),
                                                        dto.webDescription(), dto.images(),
                                                        originalPrice, discountPrice, discountPercentage,
                                                        discountName, discountColorHex,
                                                        null, null, // loyaltyPrice, loyaltyPercentage — enriched by controller
                                                        winningSource,
                                                        isPartialDiscount,
                                                        tags, dto.productCode(),
                                                        skus,
                                                        rating != null ? rating.getAverageRating() : null,
                                                        rating != null ? rating.getReviewCount() : 0,
                                                        sellerMap.get(dto.productBaseId()));
                                })
                                .toList();
        }

        @Override
        public List<String> autocomplete(String prefix, int limit) {
                Query matchQuery = Query.of(q -> q.match(m -> m.field("name.autocomplete").query(prefix)));
                Query activeFilter = Query.of(q -> q.term(t -> t.field("status").value("ACTIVE")));
                Query combined = Query.of(q -> q.bool(b -> b.must(matchQuery).filter(activeFilter)));

                NativeQuery searchQuery = NativeQuery.builder()
                                .withQuery(combined)
                                .withPageable(PageRequest.of(0, limit))
                                .build();

                SearchHits<ListingDocument> hits = operations.search(searchQuery, ListingDocument.class);

                return hits.getSearchHits().stream()
                                .map(hit -> hit.getContent().getName())
                                .distinct()
                                .toList();
        }

        // ---- Mapping ----

        private ListingVariantDto toDto(ListingDocument doc) {
                BigDecimal price = doc.getPrice() != null ? BigDecimal.valueOf(doc.getPrice()) : null;
                return new ListingVariantDto(
                                doc.getProductBaseId(),
                                doc.getId(),
                                doc.getSlug(),
                                doc.getName(),           // colorDisplayName
                                doc.getCategory(),       // categoryName
                                doc.getColorKey(),
                                doc.getColorHexCode(),   // colorHex
                                doc.getWebDescription(),
                                doc.getImages() != null ? doc.getImages() : List.of(),
                                price,   // originalPrice — discount fields enriched post-query
                                null,    // discountPrice
                                null,    // discountPercentage
                                null,    // discountName
                                null,    // discountColorHex
                                null,    // loyaltyPrice — enriched by controller
                                null,    // loyaltyPercentage
                                null,    // winningSource — recomputed in the enrichment step above
                                false,   // isPartialDiscount — enriched post-query
                                List.of(), // tags — batch-loaded post-query
                                doc.getProductCode(),
                                List.of(), // skus — batch-loaded post-query
                                null,    // ratingAverage — batch-loaded post-query
                                0,       // reviewCount — batch-loaded post-query
                                null     // sellerShopName — batch-loaded post-query
                );
        }

        private Map<Long, String> loadSellerMap(List<Long> productBaseIds) {
                if (productBaseIds.isEmpty()) return Map.of();
                List<Object[]> pairs = productBaseRepo.findSellerIdsByIds(productBaseIds);
                if (pairs.isEmpty()) return Map.of();
                Map<Long, Long> baseToSeller = pairs.stream()
                                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
                List<Long> sellerIds = new ArrayList<>(new HashSet<>(baseToSeller.values()));
                Map<Long, String> sellerNameById = sellerRepo.findAllById(sellerIds).stream()
                                .collect(Collectors.toMap(s -> s.getId(), s -> s.getShopName()));
                return baseToSeller.entrySet().stream()
                                .filter(e -> sellerNameById.containsKey(e.getValue()))
                                .collect(Collectors.toMap(Map.Entry::getKey, e -> sellerNameById.get(e.getValue())));
        }

        private Map<Long, List<TagView>> loadTagMap(List<Long> variantIds) {
                if (variantIds.isEmpty()) return Map.of();
                return variantRepo.findTagsByVariantIds(variantIds).stream()
                                .collect(Collectors.groupingBy(
                                                row -> (Long) row[0],
                                                Collectors.mapping(
                                                                row -> new TagView((Long) row[1], (String) row[2], (String) row[3]),
                                                                Collectors.toList())));
        }

        private Map<Long, ProductRatingSummaryEntity> loadRatingMap(List<Long> variantIds) {
                if (variantIds.isEmpty()) return Map.of();
                return ratingRepo.findAllById(variantIds).stream()
                                .collect(Collectors.toMap(ProductRatingSummaryEntity::getListingVariantId, e -> e));
        }

        private Map<Long, List<SkuDto>> loadSkuMap(List<Long> variantIds) {
                if (variantIds.isEmpty()) return Map.of();
                return skuRepo.findGridSkusByVariantIds(variantIds).stream()
                                .collect(Collectors.groupingBy(
                                                row -> (Long) row[0],
                                                Collectors.mapping(row -> {
                                                        BigDecimal price = row[5] instanceof BigDecimal bd ? bd
                                                                        : row[5] != null ? new BigDecimal(row[5].toString()) : null;
                                                        Integer stock = row[4] instanceof Long l ? l.intValue()
                                                                        : row[4] instanceof Integer i ? i
                                                                        : row[4] != null ? ((Number) row[4]).intValue() : 0;
                                                        return new SkuDto((Long) row[1], (String) row[2],
                                                                        (String) row[3], stock,
                                                                        price,   // originalPrice
                                                                        null,    // discountPrice
                                                                        null,    // discountPercentage
                                                                        null,    // discountName
                                                                        null,    // discountColorHex
                                                                        null,    // loyaltyPrice
                                                                        null);   // winningSource — not resolved for grid-path SKUs
                                                }, Collectors.toList())));
        }
}
