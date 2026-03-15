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
import tj.radolfa.application.readmodel.ListingVariantDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Real Elasticsearch adapter for the listings index.
 *
 * <p>
 * Handles both indexing (write) and search (read).
 * Index/delete are fire-and-forget: failures are logged but never
 * propagate to the sync pipeline.
 *
 * <p>Search results are enriched with discount data from the discounts table.
 */
@Component
@Profile("!test")
public class ListingSearchAdapter implements ListingIndexPort, SearchListingPort {

        private static final Logger LOG = LoggerFactory.getLogger(ListingSearchAdapter.class);

        private final ListingSearchRepository repository;
        private final ElasticsearchOperations operations;
        private final DiscountEnrichmentAdapter discountEnrichment;

        public ListingSearchAdapter(ListingSearchRepository repository,
                        ElasticsearchOperations operations,
                        DiscountEnrichmentAdapter discountEnrichment) {
                this.repository = repository;
                this.operations = operations;
                this.discountEnrichment = discountEnrichment;
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
                                .map(ListingVariantDto::id)
                                .toList();
                Map<Long, DiscountInfo> discountMap = discountEnrichment.resolveForVariants(variantIds);

                List<ListingVariantDto> enriched = items.stream()
                                .map(dto -> {
                                        DiscountInfo discount = discountMap.get(dto.id());
                                        if (discount == null) return dto;
                                        return new ListingVariantDto(
                                                        dto.id(), dto.slug(), dto.name(), dto.category(),
                                                        dto.colorKey(), dto.colorHexCode(), dto.webDescription(),
                                                        dto.images(),
                                                        discount.originalPrice(),
                                                        discount.discountedPrice(),
                                                        null,    // loyaltyPrice (enriched by controller)
                                                        discount.discountPercentage(),
                                                        null,    // loyaltyDiscountPercentage (enriched by controller)
                                                        discount.saleTitle(),
                                                        discount.saleColorHex(),
                                                        dto.totalStock(), dto.topSelling(), dto.featured());
                                })
                                .toList();

                long totalHits = hits.getTotalHits();
                boolean hasMore = (long) page * limit < totalHits;

                return new PageResult<>(enriched, totalHits, page, hasMore);
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
                return new ListingVariantDto(
                                doc.getId(),
                                doc.getSlug(),
                                doc.getName(),
                                doc.getCategory(),
                                doc.getColorKey(),
                                doc.getColorHexCode(),
                                doc.getWebDescription(),
                                doc.getImages() != null ? doc.getImages() : List.of(),
                                doc.getPrice() != null ? BigDecimal.valueOf(doc.getPrice()) : null,
                                null,    // discountedPrice (enriched post-query)
                                null,    // loyaltyPrice (enriched by controller)
                                null,    // discountPercentage (enriched post-query)
                                null,    // loyaltyDiscountPercentage (enriched by controller)
                                null,    // saleTitle (enriched post-query)
                                null,    // saleColorHex (enriched post-query)
                                doc.getTotalStock(),
                                doc.getTopSelling() != null && doc.getTopSelling(),
                                doc.getFeatured() != null && doc.getFeatured()
                );
        }
}
