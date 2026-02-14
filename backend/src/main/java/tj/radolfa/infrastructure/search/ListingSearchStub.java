package tj.radolfa.infrastructure.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.ListingIndexPort;
import tj.radolfa.application.ports.out.SearchListingPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.infrastructure.web.dto.ListingVariantDto;

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
    public void index(Long variantId, String slug, String name, String category,
                      String colorKey, String colorHexCode,
                      String description, List<String> images,
                      Double priceStart, Double priceEnd, Integer totalStock,
                      boolean topSelling, Instant lastSyncAt) {
        LOG.info("[LISTING-ES-STUB] Would index variant id={}, slug={}", variantId, slug);
    }

    @Override
    public void delete(String slug) {
        LOG.info("[LISTING-ES-STUB] Would delete slug={}", slug);
    }

    @Override
    public PageResult<ListingVariantDto> search(String query, int page, int limit) {
        LOG.info("[LISTING-ES-STUB] Would search for query={}", query);
        return new PageResult<>(List.of(), 0, page, false);
    }

    @Override
    public List<String> autocomplete(String prefix, int limit) {
        LOG.info("[LISTING-ES-STUB] Would autocomplete for prefix={}", prefix);
        return List.of();
    }
}
