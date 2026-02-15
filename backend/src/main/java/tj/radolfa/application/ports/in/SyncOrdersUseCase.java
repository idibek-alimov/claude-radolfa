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

    enum SyncStatus { SYNCED, SKIPPED }

    record SyncResult(SyncStatus status, String message) {

        public static SyncResult synced(String erpOrderId) {
            return new SyncResult(SyncStatus.SYNCED, "Order synced: " + erpOrderId);
        }

        public static SyncResult skipped(String reason) {
            return new SyncResult(SyncStatus.SKIPPED, reason);
        }
    }

    SyncResult execute(SyncOrderCommand command);
}
