package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.warehouse.GetStockReceiptByIdUseCase;
import tj.radolfa.application.ports.out.LoadStockReceiptPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.StockReceipt;

@Service
public class GetStockReceiptByIdService implements GetStockReceiptByIdUseCase {

    private final LoadStockReceiptPort loadStockReceiptPort;

    public GetStockReceiptByIdService(LoadStockReceiptPort loadStockReceiptPort) {
        this.loadStockReceiptPort = loadStockReceiptPort;
    }

    @Override
    @Transactional(readOnly = true)
    public StockReceipt execute(Long id) {
        return loadStockReceiptPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stock receipt not found: " + id));
    }
}
