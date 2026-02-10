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
import tj.radolfa.infrastructure.web.dto.ListingVariantDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Real Elasticsearch adapter for the listings index.
 *
 * <p>
 * Handles both indexing (write) and search (read).
 * Index/delete are fire-and-forget: failures are logged but never
 * propagate to the sync pipeline.
 */
@Component
@Profile("!test")
public class ListingSearchAdapter implements ListingIndexPort, SearchListingPort {

        private static final Logger LOG = LoggerFactory.getLogger(ListingSearchAdapter.class);

        private final ListingSearchRepository repository;
        private final ElasticsearchOperations operations;

        public ListingSearchAdapter(ListingSearchRepository repository,
                        ElasticsearchOperations operations) {
                this.repository = repository;
                this.operations = operations;
        }

        // ---- ListingIndexPort (write) ----

        @Override
        public void index(Long variantId, String slug, String name, String colorKey,
                        String description, List<String> images,
                        Double priceStart, Double priceEnd, Integer totalStock,
                        Instant lastSyncAt) {
                try {
                        ListingDocument doc = new ListingDocument(
                                        variantId, slug, name, colorKey, description,
                                        images, priceStart, priceEnd, totalStock, lastSyncAt);
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

                long totalHits = hits.getTotalHits();
                boolean hasMore = (long) page * limit < totalHits;

                return new PageResult<>(items, totalHits, page, hasMore);
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
                                doc.getColorKey(),
                                doc.getWebDescription(),
                                doc.getImages() != null ? doc.getImages() : List.of(),
                                doc.getPriceStart() != null ? BigDecimal.valueOf(doc.getPriceStart()) : null,
                                doc.getPriceEnd() != null ? BigDecimal.valueOf(doc.getPriceEnd()) : null,
                                doc.getTotalStock(),
                                false // topSelling not yet in search index
                );
        }
}
