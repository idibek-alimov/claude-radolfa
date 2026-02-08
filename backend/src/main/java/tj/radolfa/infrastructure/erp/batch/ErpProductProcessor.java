package tj.radolfa.infrastructure.erp.batch;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.in.SyncErpProductUseCase;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Product;
import tj.radolfa.infrastructure.erp.ErpProductSnapshot;

/**
 * Spring Batch {@code ItemProcessor} that delegates every snapshot
 * to {@link SyncErpProductUseCase}.
 *
 * This is the single point at which the upsert decision is made:
 * <ul>
 *   <li>Product already exists -> {@code enrichWithErpData} overwrites only locked fields.</li>
 *   <li>Product is new         -> a skeleton record is created with locked fields populated.</li>
 * </ul>
 *
 * No duplicate logic lives here – all business rules are in the use-case service.
 */
@Component
public class ErpProductProcessor implements ItemProcessor<ErpProductSnapshot, Product> {

    private final SyncErpProductUseCase syncUseCase;

    public ErpProductProcessor(SyncErpProductUseCase syncUseCase) {
        this.syncUseCase = syncUseCase;
    }

    @Override
    public Product process(ErpProductSnapshot snapshot) {
        return syncUseCase.execute(
                snapshot.erpId(),
                snapshot.name(),
                Money.of(snapshot.price()),
                snapshot.stock()
        );
    }
}
