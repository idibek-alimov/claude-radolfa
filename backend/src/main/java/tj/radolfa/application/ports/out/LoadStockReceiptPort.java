package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.StockReceipt;

import java.util.Optional;

public interface LoadStockReceiptPort {
    Optional<StockReceipt> findById(Long id);
    PageResult<StockReceipt> findAllPaged(int page, int size, String search);
}
