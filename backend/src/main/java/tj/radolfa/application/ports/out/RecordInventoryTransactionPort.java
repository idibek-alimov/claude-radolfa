package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.InventoryTransaction;

public interface RecordInventoryTransactionPort {
    void record(InventoryTransaction transaction);
}
