package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.SearchProductsUseCase;
import tj.radolfa.application.ports.out.LoadProductPort;
import tj.radolfa.application.ports.out.SearchProductPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.Product;

import java.util.List;

/**
 * Orchestrates product search: tries Elasticsearch first,
 * falls back to SQL LIKE via {@link LoadProductPort#loadPage} when ES is unavailable.
 */
@Service
public class SearchProductService implements SearchProductsUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(SearchProductService.class);

    private final SearchProductPort searchProductPort;
    private final LoadProductPort loadProductPort;

    public SearchProductService(SearchProductPort searchProductPort,
                                LoadProductPort loadProductPort) {
        this.searchProductPort = searchProductPort;
        this.loadProductPort = loadProductPort;
    }

    @Override
    public PageResult<Product> search(String query, int page, int limit) {
        try {
            return searchProductPort.search(query, page, limit);
        } catch (Exception e) {
            LOG.warn("Elasticsearch search failed, falling back to SQL: {}", e.getMessage());
            return loadProductPort.loadPage(page, limit, query);
        }
    }

    @Override
    public List<String> autocomplete(String prefix, int limit) {
        try {
            return searchProductPort.autocomplete(prefix, limit);
        } catch (Exception e) {
            LOG.warn("Elasticsearch autocomplete failed, returning empty: {}", e.getMessage());
            return List.of();
        }
    }
}
