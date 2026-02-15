package tj.radolfa.application.ports.in;

import java.math.BigDecimal;
import java.util.List;

public interface SyncOrdersUseCase {

    record SyncOrderCommand(
            String erpOrderId,
            String customerPhone,
            String status,
            BigDecimal totalAmount,
            List<SyncOrderItemCommand> items) {
    }

    record SyncOrderItemCommand(
            String erpItemCode,
            String productName,
            int quantity,
            BigDecimal price) {
    }

    void execute(SyncOrderCommand command);
}
