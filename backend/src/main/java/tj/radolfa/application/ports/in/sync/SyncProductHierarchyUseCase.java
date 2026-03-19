package tj.radolfa.application.ports.in.sync;

import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.ProductBase;

import java.util.List;

/**
 * In-Port: synchronise a full product hierarchy from the external catalogue.
 *
 * <p>Accepts a Template with its Colour Variants and Size SKUs,
 * performs idempotent upserts across all three tiers.
 *
 * <p>Called exclusively by the SYNC role (import endpoint).
 */
public interface SyncProductHierarchyUseCase {

    /**
     * Upserts the full hierarchy for a single Item Template.
     *
     * @return the ProductBase after the sync merge
     */
    ProductBase execute(HierarchySyncCommand command);

    /**
     * Command object carrying the full hierarchy payload.
     */
    record HierarchySyncCommand(
            String templateCode,
            String templateName,
            String category,
            List<VariantCommand> variants
    ) {
        public record VariantCommand(
                String colorKey,
                List<SkuCommand> items
        ) {}

        public record SkuCommand(
                String skuCode,
                String sizeLabel,
                Integer stockQuantity,
                Money listPrice
        ) {}
    }
}
