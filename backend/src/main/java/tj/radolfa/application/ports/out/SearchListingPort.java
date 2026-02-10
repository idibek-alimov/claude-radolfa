package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.PageResult;
import tj.radolfa.infrastructure.web.dto.ListingVariantDto;

import java.util.List;

/**
 * Out-Port: query the search engine for listing variant discovery.
 */
public interface SearchListingPort {

    /**
     * Full-text fuzzy search across listing name, description, and colour.
     */
    PageResult<ListingVariantDto> search(String query, int page, int limit);

    /**
     * Autocomplete suggestions based on product names.
     */
    List<String> autocomplete(String prefix, int limit);
}
