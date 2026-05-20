package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.warehouse.GetStockReceiptsUseCase;
import tj.radolfa.application.ports.out.LoadStockReceiptPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.StockReceipt;

@Service
public class GetStockReceiptsService implements GetStockReceiptsUseCase {

    private final LoadStockReceiptPort loadStockReceiptPort;

    public GetStockReceiptsService(LoadStockReceiptPort loadStockReceiptPort) {
        this.loadStockReceiptPort = loadStockReceiptPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<StockReceipt> execute(int page, int size, String search) {
        return loadStockReceiptPort.findAllPaged(page, size, search);
    }
}
