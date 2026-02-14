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
        var skuCommand = new SkuCommand(
                snapshot.erpId(),
                null,
                snapshot.stock(),
                Money.of(snapshot.price()),
                Money.of(snapshot.price()),
                null
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
}
