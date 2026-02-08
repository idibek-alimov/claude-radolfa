package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.Product;

import java.util.List;

/**
 * In-Port: search products with fuzzy matching and autocomplete.
 *
 * Uses Elasticsearch when available, falls back to SQL LIKE search.
 */
public interface SearchProductsUseCase {

    /**
     * Full-text fuzzy search across product catalog.
     *
     * @param query  the user's search text
     * @param page   1-based page number
     * @param limit  items per page
     * @return paginated results
     */
    PageResult<Product> search(String query, int page, int limit);

    /**
     * Autocomplete suggestions for the search box.
     *
     * @param prefix partial text typed by the user
     * @param limit  max number of suggestions
     * @return list of product name suggestions
     */
    List<String> autocomplete(String prefix, int limit);
}
