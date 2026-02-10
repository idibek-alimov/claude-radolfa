package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.ProductBase;

import java.time.Instant;
import java.util.List;

/**
 * In-Port: synchronise a full product hierarchy from ERPNext.
 *
 * <p>Accepts a Template with its Colour Variants and Size SKUs,
 * performs idempotent upserts across all three tiers.
 *
 * <p>Called exclusively by the SYSTEM role (ERP sync endpoint).
 */
public interface SyncProductHierarchyUseCase {

    /**
     * Upserts the full hierarchy for a single ERPNext Item Template.
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
            List<VariantCommand> variants
    ) {
        public record VariantCommand(
                String colorKey,
                List<SkuCommand> items
        ) {}

        public record SkuCommand(
                String erpItemCode,
                String sizeLabel,
                Integer stockQuantity,
                Money listPrice,
                Money effectivePrice,
                Instant saleEndsAt
        ) {}
    }
}
