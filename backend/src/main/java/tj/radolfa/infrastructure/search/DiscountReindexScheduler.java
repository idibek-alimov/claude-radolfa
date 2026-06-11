package tj.radolfa.infrastructure.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import tj.radolfa.infrastructure.persistence.adapter.DiscountEnrichmentAdapter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Periodically refreshes the Elasticsearch {@code discountPercentage} snapshot for
 * products with active discounts.
 *
 * <p>Covers campaigns that start or end purely by time ({@code validFrom}/{@code validUpto})
 * without any admin action — {@link DiscountReindexListener} only fires on explicit
 * create/update/enable/disable/delete.
 *
 * <p>Each tick reindexes the union of the currently-active set and the previous tick's
 * set, so products whose campaign just ended are recomputed to {@code discountPercentage = null}
 * and correctly drop out of the "Any discount" filter.
 */
@Component
public class DiscountReindexScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(DiscountReindexScheduler.class);

    private final DiscountEnrichmentAdapter discountEnrichment;
    private final ListingReindexService reindexService;

    private Set<Long> previouslyActive = Set.of();

    public DiscountReindexScheduler(DiscountEnrichmentAdapter discountEnrichment,
                                    ListingReindexService reindexService) {
        this.discountEnrichment = discountEnrichment;
        this.reindexService = reindexService;
    }

    @Scheduled(fixedDelayString = "${radolfa.discount.reindex-interval-ms:300000}")
    public void refreshActiveDiscounts() {
        try {
            List<Long> currentList = discountEnrichment.findVariantIdsWithActiveDiscounts();
            Set<Long> current = new HashSet<>(currentList);

            Set<Long> toReindex = new HashSet<>(current);
            toReindex.addAll(previouslyActive);

            if (!toReindex.isEmpty()) {
                LOG.debug("[REINDEX] Scheduled discount refresh: {} variants", toReindex.size());
                reindexService.reindexVariants(toReindex);
            }

            previouslyActive = current;
        } catch (Exception ex) {
            LOG.error("[ES] Scheduled discount reindex failed: {}", ex.getMessage());
        }
    }
}
