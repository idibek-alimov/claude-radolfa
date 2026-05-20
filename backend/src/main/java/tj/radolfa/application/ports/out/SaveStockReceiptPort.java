package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.StockReceipt;

public interface SaveStockReceiptPort {
    StockReceipt save(StockReceipt receipt);
}
