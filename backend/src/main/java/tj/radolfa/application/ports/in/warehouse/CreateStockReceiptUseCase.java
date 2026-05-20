package tj.radolfa.application.ports.in.warehouse;

import tj.radolfa.domain.model.StockReceipt;

import java.util.List;

public interface CreateStockReceiptUseCase {

    record ItemCommand(Long skuId, int quantity, String notes) {}

    record Command(Long adminUserId, String supplierReference, String notes,
                   List<ItemCommand> items) {}

    StockReceipt execute(Command command);
}
