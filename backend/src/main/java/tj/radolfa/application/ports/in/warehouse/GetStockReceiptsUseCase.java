package tj.radolfa.application.ports.in.warehouse;

import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.StockReceipt;

public interface GetStockReceiptsUseCase {
    PageResult<StockReceipt> execute(int page, int size, String search);
}
