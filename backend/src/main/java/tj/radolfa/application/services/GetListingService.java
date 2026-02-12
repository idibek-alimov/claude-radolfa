package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tj.radolfa.application.ports.in.GetListingUseCase;
import tj.radolfa.application.ports.out.LoadListingPort;
import tj.radolfa.application.ports.out.SearchListingPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.infrastructure.web.dto.ListingVariantDetailDto;
import tj.radolfa.infrastructure.web.dto.ListingVariantDto;

import java.util.List;
import java.util.Optional;

/**
 * Orchestrates storefront read operations.
 *
 * <p>Search uses Elasticsearch first, falls back to SQL LIKE
 * when ES is unavailable — same resilience pattern as before.
 */
@Service
@Transactional(readOnly = true)
public class GetListingService implements GetListingUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(GetListingService.class);

    private final LoadListingPort   loadListingPort;
    private final SearchListingPort searchListingPort;

    public GetListingService(LoadListingPort loadListingPort,
                             SearchListingPort searchListingPort) {
        this.loadListingPort   = loadListingPort;
        this.searchListingPort = searchListingPort;
    }

    @Override
    public PageResult<ListingVariantDto> getPage(int page, int limit) {
        return loadListingPort.loadPage(page, limit);
    }

    @Override
    public Optional<ListingVariantDetailDto> getBySlug(String slug) {
        return loadListingPort.loadBySlug(slug);
    }

    @Override
    public PageResult<ListingVariantDto> search(String query, int page, int limit) {
        try {
            return searchListingPort.search(query, page, limit);
        } catch (Exception e) {
            LOG.warn("Elasticsearch search failed, falling back to SQL: {}", e.getMessage());
            return loadListingPort.search(query, page, limit);
        }
    }

    @Override
    public List<String> autocomplete(String prefix, int limit) {
        try {
            return searchListingPort.autocomplete(prefix, limit);
        } catch (Exception e) {
            LOG.warn("Elasticsearch autocomplete failed, falling back to SQL: {}", e.getMessage());
            return loadListingPort.autocomplete(prefix, limit);
        }
    }
}
