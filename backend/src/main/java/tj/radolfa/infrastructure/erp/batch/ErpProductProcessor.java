package tj.radolfa.infrastructure.erp.batch;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.in.SyncProductHierarchyUseCase;
import tj.radolfa.application.ports.in.SyncProductHierarchyUseCase.HierarchySyncCommand;
import tj.radolfa.application.ports.in.SyncProductHierarchyUseCase.HierarchySyncCommand.VariantCommand;
import tj.radolfa.application.ports.in.SyncProductHierarchyUseCase.HierarchySyncCommand.SkuCommand;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.infrastructure.erp.ErpProductSnapshot;

import java.util.List;

/**
 * Spring Batch {@code ItemProcessor} that converts a flat ERP snapshot
 * into a hierarchy command and delegates to {@link SyncProductHierarchyUseCase}.
 *
 * <p>The flat ERP API returns individual items. This processor wraps each
 * into a minimal hierarchy: one template → one "default" variant → one SKU.
 * The use case's idempotent upsert merges correctly when multiple items
 * share the same template code.
 */
@Component
public class ErpProductProcessor implements ItemProcessor<ErpProductSnapshot, ProductBase> {

    private final SyncProductHierarchyUseCase syncUseCase;

    public ErpProductProcessor(SyncProductHierarchyUseCase syncUseCase) {
        this.syncUseCase = syncUseCase;
    }

    @Override
    public ProductBase process(ErpProductSnapshot snapshot) {
        Money listPrice = Money.of(snapshot.standardRate());
        Money effectivePrice = toEffectivePrice(snapshot);

        var skuCommand = new SkuCommand(
                snapshot.erpId(),
                null,
                snapshot.stock(),
                listPrice,
                effectivePrice,
                null,
                snapshot.discountPercentage()
        );

        var variantCommand = new VariantCommand(
                "default",
                List.of(skuCommand)
        );

        var command = new HierarchySyncCommand(
                snapshot.erpId(),
                snapshot.name(),
                snapshot.category(),
                List.of(variantCommand)
        );

        return syncUseCase.execute(command);
    }

    /**
     * Returns null when there is no real discount (discountedRate absent or equal to standardRate).
     * This ensures discounted_price stays NULL in the DB for non-promoted products.
     */
    private Money toEffectivePrice(ErpProductSnapshot snapshot) {
        if (snapshot.discountedRate() == null || snapshot.standardRate() == null) return null;
        if (snapshot.discountedRate().compareTo(snapshot.standardRate()) >= 0) return null;
        return Money.of(snapshot.discountedRate());
    }
}
