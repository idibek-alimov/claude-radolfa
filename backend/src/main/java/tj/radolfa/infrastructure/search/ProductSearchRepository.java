package tj.radolfa.infrastructure.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.Optional;

/**
 * Spring Data Elasticsearch repository for basic CRUD on the products index.
 */
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, Long> {

    Optional<ProductDocument> findByErpId(String erpId);

    void deleteByErpId(String erpId);
}
