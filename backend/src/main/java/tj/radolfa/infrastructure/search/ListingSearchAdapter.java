package tj.radolfa.infrastructure.search;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.ListingIndexPort;
import tj.radolfa.application.ports.out.SearchListingPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.infrastructure.persistence.adapter.DiscountEnrichmentAdapter;
import tj.radolfa.infrastructure.persistence.adapter.DiscountEnrichmentAdapter.DiscountInfo;
import tj.radolfa.infrastructure.persistence.repository.SkuRepository;
import tj.radolfa.application.readmodel.ListingVariantDto;
import tj.radolfa.application.readmodel.SkuDto;

import java.math.BigDecimal;
import java.time.Instant;
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

        public ListingSearchAdapter(ListingSearchRepository repository,
                        ElasticsearchOperations operations,
                        DiscountEnrichmentAdapter discountEnrichment,
                        SkuRepository skuRepo) {
                this.repository = repository;
                this.operations = operations;
                this.discountEnrichment = discountEnrichment;
                this.skuRepo = skuRepo;
        }

        // ---- ListingIndexPort (write) ----

        @Override
        public void index(Long variantId, String slug, String name, String category,
                        String colorKey, String colorHexCode,
                        String description, List<String> images,
                        Double price, Integer totalStock,
                        boolean topSelling, boolean featured, Instant lastSyncAt) {
                try {
                        ListingDocument doc = new ListingDocument(
                                        variantId, slug, name, category,
                                        colorKey, colorHexCode, description,
                                        images, price, totalStock,
                                        topSelling, featured, lastSyncAt);
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
                Query fuzzyQuery = BoolQuery.of(b -> b
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
                                                                .boost(2.0f))))
                                .minimumShouldMatch("1"))._toQuery();

                NativeQuery searchQuery = NativeQuery.builder()
                                .withQuery(fuzzyQuery)
                                .withPageable(PageRequest.of(page - 1, limit))
                                .build();

                SearchHits<ListingDocument> hits = operations.search(searchQuery, ListingDocument.class);

                List<ListingVariantDto> items = hits.getSearchHits().stream()
                                .map(SearchHit::getContent)
                                .map(this::toDto)
                                .toList();

                // Enrich with discounts from the discounts table
                List<Long> variantIds = items.stream()
                                .map(ListingVariantDto::variantId)
                                .toList();
                Map<Long, DiscountInfo> discountMap = discountEnrichment.resolveForVariants(variantIds);

                // Batch-load SKUs from DB
                Map<Long, List<SkuDto>> skuMap = loadSkuMap(variantIds);

                List<ListingVariantDto> enriched = items.stream()
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
                                        List<SkuDto> skus = skuMap.getOrDefault(dto.variantId(), List.of());
                                        return new ListingVariantDto(
                                                        dto.variantId(), dto.slug(), dto.colorDisplayName(),
                                                        dto.categoryName(), dto.colorKey(), dto.colorHex(),
                                                        dto.webDescription(), dto.images(),
                                                        originalPrice, discountPrice, discountPercentage,
                                                        discountName, discountColorHex,
                                                        null, null, // loyaltyPrice, loyaltyPercentage — enriched by controller
                                                        isPartialDiscount,
                                                        dto.topSelling(), dto.featured(), dto.productCode(),
                                                        skus);
                                })
                                .toList();

                long totalHits = hits.getTotalHits();
                boolean last = (long) page * limit >= totalHits;

                return new PageResult<>(enriched, totalHits, page, limit, last);
        }

        @Override
        public List<String> autocomplete(String prefix, int limit) {
                NativeQuery searchQuery = NativeQuery.builder()
                                .withQuery(Query.of(q -> q.match(m -> m
                                                .field("name.autocomplete")
                                                .query(prefix))))
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
                                false,   // isPartialDiscount — enriched post-query
                                doc.getTopSelling() != null && doc.getTopSelling(),
                                doc.getFeatured() != null && doc.getFeatured(),
                                null,    // productCode — not stored in ES index
                                List.of() // skus — batch-loaded post-query
                );
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
                                                                        null);   // loyaltyPrice
                                                }, Collectors.toList())));
        }
}
