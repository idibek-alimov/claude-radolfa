package tj.radolfa.infrastructure.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.Optional;

/**
 * Spring Data Elasticsearch repository for the {@code listings} index.
 */
public interface ListingSearchRepository extends ElasticsearchRepository<ListingDocument, Long> {

    Optional<ListingDocument> findBySlug(String slug);

    void deleteBySlug(String slug);
}
