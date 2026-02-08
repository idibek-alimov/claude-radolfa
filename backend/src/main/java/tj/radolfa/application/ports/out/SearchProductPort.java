package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.Product;

import java.util.List;

/**
 * Out-Port: query the search engine for product discovery.
 */
public interface SearchProductPort {

    /**
     * Full-text fuzzy search across product name, description, and erpId.
     *
     * @param query  the user's search text
     * @param page   1-based page number
     * @param limit  items per page
     * @return paginated search results
     */
    PageResult<Product> search(String query, int page, int limit);

    /**
     * Autocomplete suggestions based on product names.
     *
     * @param prefix the partial text typed by the user
     * @param limit  max number of suggestions
     * @return list of matching product names
     */
    List<String> autocomplete(String prefix, int limit);
}
