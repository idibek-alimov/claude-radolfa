package tj.radolfa.infrastructure.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import tj.radolfa.application.event.DiscountTargetsChangedEvent;
import tj.radolfa.infrastructure.persistence.adapter.DiscountEnrichmentAdapter;

import java.util.List;

/**
 * Refreshes the Elasticsearch {@code discountPercentage} snapshot for products
 * affected by a discount campaign change.
 *
 * <p>Runs after the campaign change commits, on a background thread, so the admin
 * request returns immediately even for campaigns covering thousands of products
 * (e.g. category-wide). See {@link ListingReindexService} for the batched/bulk write.
 */
@Component
public class DiscountReindexListener {

    private static final Logger LOG = LoggerFactory.getLogger(DiscountReindexListener.class);

    private final DiscountEnrichmentAdapter discountEnrichment;
    private final ListingReindexService reindexService;

    public DiscountReindexListener(DiscountEnrichmentAdapter discountEnrichment,
                                   ListingReindexService reindexService) {
        this.discountEnrichment = discountEnrichment;
        this.reindexService = reindexService;
    }

    @Async("indexingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDiscountTargetsChanged(DiscountTargetsChangedEvent event) {
        try {
            List<Long> variantIds = discountEnrichment.resolveVariantIdsForTargets(event.targets());
            if (variantIds.isEmpty()) return;
            reindexService.reindexVariants(variantIds);
        } catch (Exception ex) {
            LOG.error("[ES] Discount-triggered reindex failed: {}", ex.getMessage());
        }
    }
}
