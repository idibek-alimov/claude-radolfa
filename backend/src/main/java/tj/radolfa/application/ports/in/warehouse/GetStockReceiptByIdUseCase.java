package tj.radolfa.application.ports.in.warehouse;

import tj.radolfa.domain.model.StockReceipt;

public interface GetStockReceiptByIdUseCase {
    StockReceipt execute(Long id);
}
