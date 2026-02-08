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
import tj.radolfa.application.ports.out.ElasticsearchProductIndexer;
import tj.radolfa.application.ports.out.SearchProductPort;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.Product;

import java.math.BigDecimal;
import java.util.List;

/**
 * Real Elasticsearch adapter implementing both indexing and search.
 *
 * <p>Index/delete operations are fire-and-forget: failures are logged but never
 * propagate to the caller — the product is already safely in PostgreSQL.</p>
 */
@Component
@Profile("!test")
public class ElasticsearchProductSearchAdapter implements ElasticsearchProductIndexer, SearchProductPort {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchProductSearchAdapter.class);

    private final ProductSearchRepository repository;
    private final ElasticsearchOperations operations;

    public ElasticsearchProductSearchAdapter(ProductSearchRepository repository,
                                             ElasticsearchOperations operations) {
        this.repository = repository;
        this.operations = operations;
    }

    // ================================================================
    // ElasticsearchProductIndexer (write side)
    // ================================================================

    @Override
    public void index(Product product) {
        try {
            repository.save(toDocument(product));
            LOG.debug("Indexed product erpId={}", product.getErpId());
        } catch (Exception e) {
            LOG.warn("Failed to index product erpId={}: {}", product.getErpId(), e.getMessage());
        }
    }

    @Override
    public void delete(String erpId) {
        try {
            repository.deleteByErpId(erpId);
            LOG.debug("Deleted product from index erpId={}", erpId);
        } catch (Exception e) {
            LOG.warn("Failed to delete product from index erpId={}: {}", erpId, e.getMessage());
        }
    }

    // ================================================================
    // SearchProductPort (read side)
    // ================================================================

    @Override
    public PageResult<Product> search(String query, int page, int limit) {
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
                        Query.of(q -> q.term(t -> t
                                .field("erpId")
                                .value(query)
                                .boost(5.0f)))
                )
                .minimumShouldMatch("1")
        )._toQuery();

        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(fuzzyQuery)
                .withPageable(PageRequest.of(page - 1, limit))
                .build();

        SearchHits<ProductDocument> hits = operations.search(searchQuery, ProductDocument.class);

        List<Product> products = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toDomain)
                .toList();

        long totalHits = hits.getTotalHits();
        boolean hasMore = (long) page * limit < totalHits;

        return new PageResult<>(products, totalHits, page, hasMore);
    }

    @Override
    public List<String> autocomplete(String prefix, int limit) {
        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(Query.of(q -> q.match(m -> m
                        .field("name.autocomplete")
                        .query(prefix))))
                .withPageable(PageRequest.of(0, limit))
                .build();

        SearchHits<ProductDocument> hits = operations.search(searchQuery, ProductDocument.class);

        return hits.getSearchHits().stream()
                .map(hit -> hit.getContent().getName())
                .distinct()
                .toList();
    }

    // ================================================================
    // Mapping helpers
    // ================================================================

    private ProductDocument toDocument(Product product) {
        return new ProductDocument(
                product.getId(),
                product.getErpId(),
                product.getName(),
                product.getPrice() != null ? product.getPrice().amount().doubleValue() : null,
                product.getStock(),
                product.getWebDescription(),
                product.isTopSelling(),
                product.getImages(),
                product.getLastErpSyncAt()
        );
    }

    private Product toDomain(ProductDocument doc) {
        return new Product(
                doc.getId(),
                doc.getErpId(),
                doc.getName(),
                doc.getPrice() != null ? Money.of(BigDecimal.valueOf(doc.getPrice())) : null,
                doc.getStock(),
                doc.getWebDescription(),
                doc.isTopSelling(),
                doc.getImages(),
                doc.getLastErpSyncAt()
        );
    }
}
