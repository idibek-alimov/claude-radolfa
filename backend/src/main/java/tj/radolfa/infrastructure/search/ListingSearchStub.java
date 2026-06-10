package tj.radolfa.infrastructure.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.ListingIndexPort;
import tj.radolfa.application.ports.out.SearchListingPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.application.readmodel.CatalogFacets;
import tj.radolfa.application.readmodel.CatalogResult;
import tj.radolfa.application.readmodel.ListingQueryCriteria;
import tj.radolfa.application.readmodel.ListingVariantDto;

import java.time.Instant;
import java.util.List;

/**
 * No-op stub for listing search ports.
 * Active only on the {@code test} profile so integration tests
 * don't require a running Elasticsearch instance.
 */
@Component
@Profile("test")
public class ListingSearchStub implements ListingIndexPort, SearchListingPort {

    private static final Logger LOG = LoggerFactory.getLogger(ListingSearchStub.class);

    @Override
    public void index(Long variantId, Long productBaseId, String slug, String name, String category,
                      String colorKey, String colorHexCode,
                      String description, List<String> images,
                      Double price, Integer totalStock,
                      Instant lastSyncAt,
                      String productCode, List<String> skuCodes,
                      String status,
                      Long categoryId, Long brandId, String brandName,
                      Integer discountPercentage, Double ratingAverage,
                      Instant createdAt) {
        LOG.info("[LISTING-ES-STUB] Would index variant id={}, slug={}", variantId, slug);
    }

    @Override
    public void delete(String slug) {
        LOG.info("[LISTING-ES-STUB] Would delete slug={}", slug);
    }

    @Override
    public PageResult<ListingVariantDto> search(String query, int page, int limit) {
        LOG.info("[LISTING-ES-STUB] Would search for query={}", query);
        return new PageResult<>(List.of(), 0, page, limit, true);
    }

    @Override
    public List<String> autocomplete(String prefix, int limit) {
        LOG.info("[LISTING-ES-STUB] Would autocomplete for prefix={}", prefix);
        return List.of();
    }

    @Override
    public CatalogResult searchCatalog(ListingQueryCriteria criteria, int page, int limit) {
        LOG.info("[LISTING-ES-STUB] Would search catalog for criteria={}", criteria);
        return new CatalogResult(new PageResult<>(List.of(), 0, page, limit, true), CatalogFacets.empty());
    }
}
