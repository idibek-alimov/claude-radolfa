package tj.radolfa.infrastructure.erp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.ElasticsearchProductIndexer;
import tj.radolfa.application.ports.out.SearchProductPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.Product;

import java.util.List;

/**
 * No-op stub for {@link ElasticsearchProductIndexer} and {@link SearchProductPort}.
 *
 * Active only on {@code test} profile. The real Elasticsearch adapter
 * handles both indexing and search in dev/prod.
 */
@Component
@Profile("test")
public class ElasticsearchProductIndexerStub implements ElasticsearchProductIndexer, SearchProductPort {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchProductIndexerStub.class);

    @Override
    public void index(Product product) {
        LOG.info("[ES-STUB] Would index product erpId={}, name={}", product.getErpId(), product.getName());
    }

    @Override
    public void delete(String erpId) {
        LOG.info("[ES-STUB] Would delete product erpId={}", erpId);
    }

    @Override
    public PageResult<Product> search(String query, int page, int limit) {
        LOG.info("[ES-STUB] Would search for query={}", query);
        return new PageResult<>(List.of(), 0, page, false);
    }

    @Override
    public List<String> autocomplete(String prefix, int limit) {
        LOG.info("[ES-STUB] Would autocomplete for prefix={}", prefix);
        return List.of();
    }
}
