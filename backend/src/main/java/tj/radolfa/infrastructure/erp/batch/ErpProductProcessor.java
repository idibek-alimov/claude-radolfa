package tj.radolfa.infrastructure.erp.batch;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.in.SyncProductHierarchyUseCase.HierarchySyncCommand;
import tj.radolfa.application.ports.in.SyncProductHierarchyUseCase.HierarchySyncCommand.VariantCommand;
import tj.radolfa.application.ports.in.SyncProductHierarchyUseCase.HierarchySyncCommand.SkuCommand;
import tj.radolfa.domain.model.Money;
import tj.radolfa.infrastructure.erp.ErpProductSnapshot;

import java.util.List;

/**
 * Spring Batch {@code ItemProcessor} that converts a flat ERP snapshot
 * into a hierarchy command.
 *
 * <p>The flat ERP API returns individual items. This processor wraps each
 * into a minimal hierarchy: one template → one "default" variant → one SKU.
 * Persistence is handled by {@link ErpProductWriter}.
 */
@Component
public class ErpProductProcessor implements ItemProcessor<ErpProductSnapshot, HierarchySyncCommand> {

    @Override
    public HierarchySyncCommand process(ErpProductSnapshot snapshot) {
        if (snapshot.disabled()) {
            return null; // Spring Batch skips null returns
        }

        Money listPrice = Money.of(snapshot.standardRate());

        var skuCommand = new SkuCommand(
                snapshot.erpId(),
                null,
                snapshot.stock(),
                listPrice
        );

        var variantCommand = new VariantCommand(
                "default",
                List.of(skuCommand)
        );

        return new HierarchySyncCommand(
                snapshot.erpId(),
                snapshot.name(),
                snapshot.category(),
                List.of(variantCommand)
        );
    }
}
