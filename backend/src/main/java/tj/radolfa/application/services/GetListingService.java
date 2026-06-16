package tj.radolfa.application.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tj.radolfa.application.ports.in.GetListingUseCase;
import tj.radolfa.application.ports.out.LoadListingPort;
import tj.radolfa.application.ports.out.SearchListingPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.application.readmodel.CatalogFacets;
import tj.radolfa.application.readmodel.CatalogResult;
import tj.radolfa.application.readmodel.ListingQueryCriteria;
import tj.radolfa.application.readmodel.ListingVariantDetailDto;
import tj.radolfa.application.readmodel.ListingVariantDto;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Orchestrates storefront read operations.
 *
 * <p>Search uses Elasticsearch first, falls back to SQL LIKE
 * when ES is unavailable — same resilience pattern as before.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class GetListingService implements GetListingUseCase {

    private static final Pattern PRODUCT_CODE  = Pattern.compile("^\\d{5,}$");
    private static final int     MAX_PAGE_SIZE = 100;

    private final LoadListingPort   loadListingPort;
    private final SearchListingPort searchListingPort;

    public GetListingService(LoadListingPort loadListingPort,
                             SearchListingPort searchListingPort) {
        this.loadListingPort   = loadListingPort;
        this.searchListingPort = searchListingPort;
    }

    @Override
    public PageResult<ListingVariantDto> getPage(int page, int limit) {
        return loadListingPort.loadPage(page, Math.min(limit, MAX_PAGE_SIZE));
    }

    @Override
    public Optional<ListingVariantDetailDto> getBySlug(String slug) {
        return loadListingPort.loadBySlug(slug);
    }

    @Override
    public PageResult<ListingVariantDto> search(String query, int page, int limit) {
        // Exact product-code lookup: bypass Elasticsearch entirely for digit-only article codes.
        if (query != null && PRODUCT_CODE.matcher(query.trim()).matches()) {
            return loadListingPort.findByProductCode(query.trim(), page, limit);
        }
        int safeLimit = Math.min(limit, MAX_PAGE_SIZE);
        try {
            return searchListingPort.search(query, page, safeLimit);
        } catch (Exception e) {
            log.warn("Elasticsearch search failed, falling back to SQL: {}", e.getMessage());
            return loadListingPort.search(query, page, safeLimit);
        }
    }

    @Override
    public List<String> autocomplete(String prefix, int limit) {
        try {
            return searchListingPort.autocomplete(prefix, limit);
        } catch (Exception e) {
            log.warn("Elasticsearch autocomplete failed, falling back to SQL: {}", e.getMessage());
            return loadListingPort.autocomplete(prefix, limit);
        }
    }

    @Override
    public PageResult<ListingVariantDto> getByCategoryIds(List<Long> categoryIds, int page, int limit) {
        return loadListingPort.loadByCategoryIds(categoryIds, page, Math.min(limit, MAX_PAGE_SIZE));
    }

    @Override
    public CatalogResult searchCatalog(ListingQueryCriteria criteria, int page, int limit) {
        // Exact product-code lookup: bypass Elasticsearch entirely for digit-only article codes.
        if (criteria.hasQuery() && PRODUCT_CODE.matcher(criteria.query().trim()).matches()) {
            PageResult<ListingVariantDto> codeResult =
                    loadListingPort.findByProductCode(criteria.query().trim(), page, limit);
            return new CatalogResult(codeResult, CatalogFacets.empty());
        }
        int safeLimit = Math.min(limit, MAX_PAGE_SIZE);
        try {
            return searchListingPort.searchCatalog(criteria, page, safeLimit);
        } catch (Exception e) {
            log.warn("Elasticsearch catalog search failed, falling back to SQL: {}", e.getMessage());
            return loadListingPort.searchCatalog(criteria, page, safeLimit);
        }
    }
}
