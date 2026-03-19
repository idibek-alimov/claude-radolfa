package tj.radolfa.infrastructure.importer.batch;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.in.sync.SyncProductHierarchyUseCase.HierarchySyncCommand;
import tj.radolfa.application.ports.in.sync.SyncProductHierarchyUseCase.HierarchySyncCommand.VariantCommand;
import tj.radolfa.application.ports.in.sync.SyncProductHierarchyUseCase.HierarchySyncCommand.SkuCommand;
import tj.radolfa.domain.model.Money;
import tj.radolfa.infrastructure.importer.ImportedProductSnapshot;

import java.util.List;

/**
 * Spring Batch {@code ItemProcessor} that converts a flat product snapshot
 * into a hierarchy command.
 *
 * <p>The flat catalogue API returns individual items. This processor wraps each
 * into a minimal hierarchy: one template → one "default" variant → one SKU.
 * Persistence is handled by {@link ImportedProductWriter}.
 */
@Component
public class ImportedProductProcessor implements ItemProcessor<ImportedProductSnapshot, HierarchySyncCommand> {

    @Override
    public HierarchySyncCommand process(ImportedProductSnapshot snapshot) {
        if (snapshot.disabled()) {
            return null; // Spring Batch skips null returns
        }

        Money listPrice = Money.of(snapshot.standardRate());

        var skuCommand = new SkuCommand(
                snapshot.importId(),
                null,
                snapshot.stock(),
                listPrice
        );

        var variantCommand = new VariantCommand(
                "default",
                List.of(skuCommand)
        );

        return new HierarchySyncCommand(
                snapshot.importId(),
                snapshot.name(),
                snapshot.category(),
                List.of(variantCommand)
        );
    }
}
